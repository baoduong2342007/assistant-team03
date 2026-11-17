package vn.team03.bepnha

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AgentToolsTest {
    private val engine = AssistantTestCatalog.engine
    private val day = AssistantTestCatalog.day
    private val reads = AgentReadTools(engine, MealPlanning(engine.recipes))

    @Test fun specsAreConsistent() {
        assertEquals(setOf("propose_actions", "plan_meals", "suggest_meals"), AgentTools.terminal)
        AgentTools.specs.forEach { spec -> assertTrue(spec.name, spec.parameters.keys.containsAll(spec.optional)) }
        val actionType = AgentTools.specs.single { it.name == "propose_actions" }.parameters.getValue("actions").items!!.properties.getValue("type")
        assertEquals(AssistantActionType.entries.map { it.name }, actionType.values)
        // Every field an action accepts is declared to the model.
        val declared = AgentTools.specs.single { it.name == "propose_actions" }.parameters.getValue("actions").items!!.properties.keys
        assertTrue(declared.containsAll(AssistantGateway.ACTION_FIELDS.values.flatten()))
    }

    @Test fun searchFiltersByKindIngredientAndTime() {
        val soups = reads.run("search_recipes", JSONObject().put("kind", "soup").put("exclude_ingredients", org.json.JSONArray(listOf("cá"))), AssistantTestCatalog.state(), day)
            .getJSONArray("recipes")
        assertTrue(soups.length() > 0)
        (0 until soups.length()).map { soups.getJSONObject(it) }.forEach { recipe ->
            assertEquals("soup", recipe.getString("kind"))
            assertFalse(recipe.getJSONArray("ingredients").toString().contains("Cá basa"))
        }
        val none = reads.run("search_recipes", JSONObject().put("query", "phở bò"), AssistantTestCatalog.state(), day)
        assertEquals(0, none.getJSONArray("recipes").length()); assertTrue(none.getString("note").contains("Không"))
    }

    @Test fun shoppingListCoversOneMealOrTheWholeDay() {
        val state = AssistantTestCatalog.state()
        val meal = reads.run("get_shopping_list", JSONObject().put("date", day.toString()).put("meal", "lunch"), state, day).getJSONArray("items")
        val whole = reads.run("get_shopping_list", JSONObject().put("date", day.toString()), state, day).getJSONArray("items")
        assertTrue(whole.length() > meal.length())
    }

    @Test fun readToolsRejectDaysOutsideTheWindowAndUnknownTools() {
        assertThrows(IllegalArgumentException::class.java) {
            reads.run("get_cooking_plan", JSONObject().put("date", day.plusDays(9).toString()).put("meal", "dinner"), AssistantTestCatalog.state(), day)
        }
        assertThrows(IllegalArgumentException::class.java) { reads.run("drop_tables", JSONObject(), AssistantTestCatalog.state(), day) }
    }
}
