package vn.team03.bepnha

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MealPlanningTest {
    private val rules = MealPlanning(AssistantTestCatalog.engine.recipes)
    private val day = AssistantTestCatalog.day

    @Test fun dateAndMealRegressionCases() {
        listOf(
            Triple("toi thu ba doi mon canh", 1L, Meal.DINNER),
            Triple("trua chu nhat doi mon rau", 6L, Meal.LUNCH),
            Triple("Đổi một món canh cho bữa này", 0L, Meal.LUNCH),
            Triple("Tối mốt đổi món canh", 2L, Meal.DINNER),
            Triple("Đổi sang ngày mai bữa tối", 1L, Meal.DINNER),
            Triple("doi sang ngay mai bua toi", 1L, Meal.DINNER),
            Triple("tối ngày mai nữa", 2L, Meal.DINNER),
        ).forEach { (text, offset, meal) ->
            assertEquals(text, MenuScope(day.plusDays(offset), meal), rules.scope(text, AssistantTestCatalog.context()))
        }
    }

    @Test fun clarificationRetainsOriginalDayAndDish() {
        val history = listOf(AssistantChatMessage("user", "Ngày mai mình muốn ăn canh chua cá"), AssistantChatMessage("assistant", MealPlanning.MEAL_QUESTION))
        val previous = rules.previousQuestion(history)
        assertEquals(MenuScope(day.plusDays(1), Meal.DINNER), rules.scope("Bữa tối nhé, giữ món mặn và rau", AssistantTestCatalog.context(), previous))
        assertEquals(mapOf(1 to "sour"), rules.requestedRecipes(previous))
    }

    @Test fun explicitDateCrossesYearAndRejectsInvalidDays() {
        val current = LocalDate.of(2026, 12, 30)
        assertEquals(setOf(LocalDate.of(2027, 1, 2)), rules.mentionedDays("Ngày 2/1", current))
        assertNotNull(rules.scope("Ngày 31/2 bữa tối", AssistantTestCatalog.context()).clarification)
        assertNotNull(rules.scope("Tối 2026-12-01", AssistantTestCatalog.context()).clarification)
        assertNotNull(rules.scope("Bữa sáng ngày mai", AssistantTestCatalog.context()).clarification)
        assertNotNull(rules.scope("Trưa và tối mai", AssistantTestCatalog.context()).clarification)
    }

    @Test fun pendingAndConfirmedEditsKeepTheirTarget() {
        listOf("pending_menu_changes", "last_menu_changes").forEach { key ->
            val context = AssistantTestCatalog.context().put(key, JSONArray().put(AssistantTestCatalog.swapJson()))
            assertEquals(MenuScope(day.plusDays(1), Meal.DINNER), rules.scope("Đổi món đó", context))
            assertEquals(1, rules.referencedSlot(context, rules.scope("Đổi món đó", context)))
        }
    }

    @Test fun negatedListsLatestAliasesAndExhaustiveInventory() {
        listOf(
            listOf("Tủ lạnh không còn thịt heo, trứng. Chỉ còn rau muống."),
            listOf("Hết thịt lợn, còn rau muống.", "Tủ lạnh có thịt heo."),
            listOf("Chỉ còn rau muống.", "Tủ lạnh có trứng."),
            listOf("Có rau muống, không có thịt heo và trứng."),
            listOf("Trứng đã hết, còn rau muống"),
        ).forEach { messages -> assertEquals(messages.toString(), setOf("Rau muống"), rules.pantryNames(listOf("thịt heo", "trứng", "rau muống"), messages)) }
    }

    @Test fun additiveInventoryAndOverlappingNames() {
        assertEquals(setOf("Trứng gà", "Rau muống"), rules.pantryNames(listOf("trứng gà", "rau muống"), listOf("Mới mua thêm rau muống", "Tủ lạnh có trứng")))
        assertEquals(setOf("Cà chua"), rules.pantryNames(listOf("cá", "cà chua"), listOf("Có cà chua")))
        assertEquals(setOf("Bông cải xanh"), rules.pantryNames(listOf("cải xanh", "bông cải xanh"), listOf("Có bông cải xanh")))
        assertEquals(setOf("Rau muống"), rules.constraints(listOf("Tủ lạnh có rau muống. Tối mai nấu gì, tránh trứng?")).pantry)
        assertEquals(setOf("Tỏi"), rules.constraints(listOf("Tôi có tỏi. Tối mai nấu gì?")).pantry)
    }

    @Test fun inventoryOnlyDoesNotMeanForbidBuyingIngredients() {
        assertFalse(rules.constraints(listOf("Chỉ còn rau muống")).only)
        assertTrue(rules.constraints(listOf("Chỉ dùng rau muống, không mua thêm")).only)
        assertFalse(rules.constraints(listOf("Có thể mua thêm", "Chỉ dùng trứng")).only)
        assertTrue(rules.constraints(listOf("Tránh trứng và lạc")).unknownExclusion)
        assertFalse(rules.constraints(listOf("Tránh trứng và thịt heo nhé")).unknownExclusion)
    }

    @Test fun rankingIsDeterministicAndRespectsExclusions() {
        val pantry = rules.ingredientNames(listOf("thịt heo", "rau muống", "trứng"))
        val excluded = setOf("Trứng gà")
        val options = rules.rankMeals(pantry, excluded)
        assertTrue(options.isNotEmpty())
        assertEquals(options, rules.rankMeals(pantry, excluded))
        options.forEach { option ->
            val menu = option.recipeIds.map(AssistantTestCatalog.engine.recipes::getValue)
            assertEquals(RecipeKind.entries.toList(), menu.map { it.kind })
            assertFalse(menu.flatMap { it.ingredients }.any { it.name in excluded })
            assertTrue(option.pantryKeys.isNotEmpty())
        }
        assertTrue(rules.rankMeals(emptySet(), emptySet()).isEmpty())
    }

    @Test fun namedSlotsRespectDoNotChange() {
        assertEquals(setOf(1), rules.namedSlots("Đừng thay canh, giữ món mặn và món rau", "(?:dung|khong|khoi) (?:doi|thay)"))
        assertEquals(setOf(0, 2), rules.namedSlots("Đổi canh, giữ nguyên món mặn và món rau", "giu(?: nguyen)?"))
        assertEquals(emptyMap<Int, String>(), rules.requestedRecipes("Đổi thành canh chua cá hay canh cải xanh thịt bằm"))
    }
}
