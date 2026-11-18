package vn.team03.bepnha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Guards the shipped recipes.json: the planner, shopping list and diet filters all trust it. */
class RecipeCatalogTest {
    private val text = checkNotNull(javaClass.getResourceAsStream("/recipes.json")).bufferedReader().use { it.readText() }
    private val raw = JSONObject(text).getJSONArray("recipes").let { items -> (0 until items.length()).map(items::getJSONObject) }
    private val engine = RecipeRepository.parse(text)
    private val recipes = engine.recipes.values

    @Test fun idsAreUniqueAndOriginalIdsAreKept() {
        val ids = raw.map { it.getString("id") }
        assertEquals(ids.groupBy { it }.filterValues { it.size > 1 }.keys.toString(), ids.size, ids.toSet().size)
        // Saved plans, favorites and reminders reference these ids.
        val original = listOf("pork", "soup", "greens", "chicken", "fish", "tofu", "shrimp", "eggs",
            "squash", "sour", "cabbage", "broccoli", "boiled", "cucumber")
        assertTrue((original - ids.toSet()).toString(), ids.containsAll(original))
    }

    @Test fun kindsAreValidAndEveryKindHasChoices() {
        raw.forEach { assertTrue(it.getString("id"), it.getString("kind") in setOf("main", "soup", "veg", "breakfast")) }
        RecipeKind.trayKinds.forEach { kind -> assertTrue(kind.name, recipes.count { it.kind == kind } >= 14) }
    }

    @Test fun recipesAreComplete() {
        recipes.forEach { recipe ->
            assertTrue(recipe.id, recipe.name.isNotBlank() && recipe.emoji.isNotBlank() && recipe.note.isNotBlank() && recipe.tip.isNotBlank())
            assertTrue(recipe.id, recipe.minutes > 0 && recipe.cost > 0)
            assertTrue(recipe.id, recipe.steps.isNotEmpty() && recipe.steps.all { it.isNotBlank() })
            assertTrue(recipe.id, recipe.ingredients.isNotEmpty() && recipe.ingredients.all { it.amount > 0 })
            assertEquals(recipe.id, recipe.ingredients.size, recipe.ingredients.map { it.name }.toSet().size)
        }
    }

    @Test fun sameIngredientNameUsesOneGroupAndUnit() {
        val groups = setOf("Thịt, cá & trứng", "Rau củ & trái cây", "Gia vị & đồ khô")
        val spoons = setOf("muỗng canh", "muỗng cà phê")
        recipes.flatMap { it.ingredients }.groupBy { it.name }.forEach { (name, uses) ->
            assertEquals(name, 1, uses.map { it.group }.toSet().size)
            assertTrue(name, uses.first().group in groups)
            val units = uses.map { it.unit }.toSet()
            // Seasonings already measure in both spoon sizes; everything else must merge on one key.
            assertTrue("$name: $units", units.size == 1 || (spoons.containsAll(units) && uses.first().group == "Gia vị & đồ khô"))
        }
    }

    @Test fun combinationsAreValidDistinctAndCoverTheCatalog() {
        assertTrue(engine.combinations.size >= 14)
        engine.combinations.forEach { assertTrue(it.toString(), engine.validPlan(it)) }
        assertEquals(engine.combinations.size, engine.combinations.toSet().size)
        assertEquals(6, engine.breakfasts.size)
        engine.breakfasts.forEach { assertTrue(engine.validPlan(listOf(it), Meal.BREAKFAST)) }
        assertEquals(emptySet<String>(), engine.recipes.keys - engine.combinations.flatten().toSet() - engine.breakfasts.toSet())
    }

    @Test fun tagsAllergensAndSodiumUseKnownValues() {
        raw.forEach { item -> listOf("tags", "allergens").forEach { key -> assertNotNull("${item.getString("id")} $key", item.optJSONArray(key)) } }
        recipes.forEach { recipe ->
            assertTrue("${recipe.id}: ${recipe.tags}", Recipe.TAGS.containsAll(recipe.tags))
            assertTrue("${recipe.id}: ${recipe.allergens}", Recipe.ALLERGENS.containsAll(recipe.allergens))
            assertTrue("${recipe.id}: ${recipe.sodium}", recipe.sodium in Recipe.SODIUM || (recipe.kind == RecipeKind.BREAKFAST && recipe.sodium.isEmpty()))
            if ("low_salt" in recipe.tags) assertEquals(recipe.id, "low", recipe.sodium)
        }
    }

    @Test fun dietLabelsMatchIngredients() {
        val allergenOf = mapOf("Tôm" to "shrimp", "Cá basa" to "fish", "Cá thu" to "fish", "Cá lóc" to "fish",
            "Cá diêu hồng" to "fish", "Trứng gà" to "egg", "Đậu hũ" to "soy", "Nước tương" to "soy")
        fun animal(name: String) = name == "Nước mắm" || listOf("Thịt", "Cá ", "Trứng", "Tôm", "Sườn").any(name::startsWith)
        recipes.forEach { recipe ->
            val names = recipe.ingredients.map { it.name }
            val expected = names.mapNotNull(allergenOf::get).toSet()
            assertTrue("${recipe.id} misses ${expected - recipe.allergens}", recipe.allergens.containsAll(expected))
            names.filter { it.startsWith("Cá ") }.forEach { assertTrue("$it needs an allergen entry", it in allergenOf) }
            if ("vegetarian" in recipe.tags) assertTrue("${recipe.id}: ${names.filter(::animal)}", names.none(::animal))
        }
    }

    @Test fun noRecipeNameIsAPrefixOfAnother() {
        // Exact-name requests ("đổi sang <tên món>") match by prefix, so one name must not start another.
        val names = recipes.map { MealPlanning.normalize(it.name) }
        names.forEach { a -> names.filter { it != a }.forEach { b -> assertFalse("$a / $b", "$b ".startsWith("$a ")) } }
        assertEquals(names.size, names.toSet().size)
    }
}
