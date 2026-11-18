package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AssistantUndoTest {
    private val day = LocalDate.of(2026, 11, 3)
    private val base = PlannerState(date = day)

    @Test fun undoRestoresDataButKeepsTheCurrentSelection() {
        val undo = AssistantUndo()
        val after = base.copy(favorites = setOf("tofu"))
        undo.push(AssistantChange(base, after, "Lưu món"))
        val viewing = after.copy(date = day.plusDays(2), meal = Meal.LUNCH)
        assertTrue(undo.canUndo(viewing))
        val restored = requireNotNull(undo.pop(viewing)).before
        assertEquals(emptySet<String>(), restored.favorites)
        assertEquals(day.plusDays(2), restored.date); assertEquals(Meal.LUNCH, restored.meal)
    }

    @Test fun manualEditAfterwardsBlocksUndoAndClearsHistory() {
        val undo = AssistantUndo()
        val first = base.copy(favorites = setOf("tofu"))
        val second = first.copy(favorites = setOf("tofu", "eggs"))
        undo.push(AssistantChange(base, first, "a")); undo.push(AssistantChange(first, second, "b"))
        val manual = second.copy(reminders = setOf("x"))
        assertFalse(undo.canUndo(manual))
        assertNull(undo.pop(manual))
        assertFalse(undo.canUndo(second))
    }

    @Test fun chainedChangesUndoInReverseOrderWithinTheLimit() {
        val undo = AssistantUndo(limit = 2)
        val states = (0..3).map { base.copy(profile = base.profile.copy(people = it + 1)) }
        states.zipWithNext().forEach { (before, after) -> undo.push(AssistantChange(before, after, "")) }
        assertEquals(states[2], undo.pop(states[3])?.before)
        assertEquals(states[1], undo.pop(states[2])?.before)
        assertNull(undo.pop(states[1]))
    }

    @Test fun noOpChangesAreNotRecorded() {
        val undo = AssistantUndo()
        undo.push(AssistantChange(base, base.copy(meal = Meal.LUNCH), "chỉ chọn bữa"))
        assertFalse(undo.canUndo(base))
    }
}
