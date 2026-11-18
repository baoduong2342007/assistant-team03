package vn.team03.bepnha

/** One change the assistant applied, with the data before and after it. */
data class AssistantChange(val before: PlannerState, val after: PlannerState, val label: String)

/**
 * Undo for assistant changes. A change can be undone only while the saved data still equals what
 * the assistant left (the selected day/meal is ignored), so manual edits made later are never lost.
 */
class AssistantUndo(private val limit: Int = 5) {
    private val stack = ArrayDeque<AssistantChange>()

    fun push(change: AssistantChange) {
        if (change.before.sameData(change.after)) return
        stack.addLast(change)
        while (stack.size > limit) stack.removeFirst()
    }

    fun canUndo(current: PlannerState): Boolean = stack.lastOrNull()?.after?.sameData(current) == true

    /** The state to restore (keeping the current selection), or null when undo is no longer safe. */
    fun pop(current: PlannerState): AssistantChange? {
        if (!canUndo(current)) { stack.clear(); return null }
        val change = stack.removeLast()
        return change.copy(before = change.before.copy(date = current.date, meal = current.meal))
    }

    fun clear() = stack.clear()

    private fun PlannerState.sameData(other: PlannerState) = copy(date = other.date, meal = other.meal) == other
}

/** Actions that need the Activity (system share sheet, calendar app, permission screens). */
sealed interface PlannerEffect {
    data class Share(val text: String) : PlannerEffect
    data class Calendar(val tasks: List<CookingTask>) : PlannerEffect
    data object Notifications : PlannerEffect
    data object ExactAlarms : PlannerEffect
}

/** A group of dish swaps for one meal in a proposal, shown and removable as a unit. */
data class ProposalMealUi(val date: java.time.LocalDate, val meal: Meal, val title: String, val lines: List<String>)
