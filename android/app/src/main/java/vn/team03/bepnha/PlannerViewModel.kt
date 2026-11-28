package vn.team03.bepnha

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import vn.team03.bepnha.voice.CookCommand
import vn.team03.bepnha.voice.LiveCall
import vn.team03.bepnha.voice.LiveCookController
import vn.team03.bepnha.voice.LiveCookEngine
import vn.team03.bepnha.voice.LiveCookSetup
import vn.team03.bepnha.voice.LiveCookState
import vn.team03.bepnha.voice.VoiceCommand
import vn.team03.bepnha.voice.VoiceController
import vn.team03.bepnha.voice.VoiceState
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class PlannerViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    val engine = RecipeRepository(application).engine
    private val storage = PlannerStorage(application, engine)
    var today by mutableStateOf(PlannerEngine.today()); private set
    var state by mutableStateOf(storage.load(today)); private set
    var showWelcome by mutableStateOf(!storage.welcomeSeen()); private set
    var screen by mutableStateOf(Screen.TODAY); private set
    var sheet by mutableStateOf<PlannerSheet?>(null); private set
    internal var sheetVersion by mutableIntStateOf(0); private set
    internal var notificationVersion by mutableIntStateOf(0); private set
    var now by mutableLongStateOf(System.currentTimeMillis()); private set
    var message by mutableStateOf<String?>(null); private set
    var notifications by mutableStateOf(ReminderStore.canNotify(application)); private set
    var exactAlarms by mutableStateOf(ReminderStore.canScheduleExact(application)); private set

    // "Hỏi Bếp Nhà" keeps its drafts and conversation while the user moves between tabs.
    var askTab by mutableStateOf(AskTab.FRIDGE); private set
    var fridgeItems by mutableStateOf(listOf<String>()); private set
    var fridgeDraft by mutableStateOf("")
    var fridgeMeal by mutableStateOf(if (state.meal.tray) state.meal else Meal.DINNER); private set
    var assistantVisible by mutableStateOf(false); private set
    private val chatStore = ChatStore(application)
    private val chatSaves = Channel<List<AssistantChatMessage>>(Channel.CONFLATED)
    private var historyState by mutableStateOf(chatStore.load(engine.recipes.keys))
    /** Restored from the device on start (text only); every change is saved in the background. */
    var assistantHistory: List<AssistantChatMessage>
        get() = historyState
        private set(value) {
            historyState = value
            chatSaves.trySend(ChatStore.keep(value, unansweredMessageIds.toSet()))
        }
    private var questionState by mutableStateOf(savedState.get<String>("assistant-draft").orEmpty())
    var assistantQuestion: String
        get() = questionState
        set(value) { questionState = value.take(1000); savedState["assistant-draft"] = questionState }
    var assistantBusy by mutableStateOf(false); private set
    var assistantError by mutableStateOf<String?>(null); private set
    var assistantProposal by mutableStateOf<AssistantProposal?>(null); private set
    var assistantRecipeId by mutableStateOf<String?>(null); private set
    private data class MealSuggestions(val base: PlannerState, val options: List<AssistantMealSuggestion>, val messageId: String, val requestId: String, val constraints: PantryConstraints?)
    private var offeredMeals by mutableStateOf<MealSuggestions?>(null)
    val assistantMealSuggestions get() = offeredMeals?.options.orEmpty()
    val assistantMealMessageId get() = offeredMeals?.messageId
    private var revisingProposal by mutableStateOf<AssistantProposal?>(null)
    val assistantRevisionTarget: String?
        get() = revisingProposal?.actions?.firstOrNull()?.let { action ->
            "${action.meal.title} · ${PlannerEngine.fullDate(action.date)}"
        }
    private var lastMenuChanges = emptyList<AssistantAction>()
    private var assistantOrigin = Screen.TODAY
    private var returnRecipeId: String? = null
    private data class Turn(
        val prompt: String,
        val history: List<AssistantChatMessage>,
        val base: PlannerState,
        val today: LocalDate,
        val origin: Screen,
        val recipeId: String?,
        val pendingChanges: List<AssistantAction>,
        val lastChanges: List<AssistantAction>,
        val suggestions: MealSuggestions?,
        val comparing: Boolean,
        val messageId: String,
    )
    private var generation = 0L
    private var activeJob: Job? = null
    private var activeTurn: Turn? = null
    private var failedTurn: Turn? = null
    private val unansweredMessageIds = mutableSetOf<String>()
    var failedMessageId by mutableStateOf<String?>(null); private set
    val assistantConfigured get() = BuildConfig.DEBUG && BuildConfig.FIREBASE_CONFIGURED
    val canRetryAssistant get() = failedMessageId != null && !assistantBusy && assistantProposal == null
    private val assistantService = AssistantService(engine, if (assistantConfigured) FirebaseAssistantProvider(application) else null)
    internal var assistantTransport: (suspend (String, List<AssistantChatMessage>, org.json.JSONObject) -> AssistantReply)? = null
    private val commands = AssistantCommands(engine)
    private val updates = Channel<Pair<PlannerState, String?>>(Channel.UNLIMITED)
    private val undo = AssistantUndo()
    /** Reads [state], so it recomposes whenever the data the last change left behind is edited. */
    val canUndoAssistant get() = undo.canUndo(state)
    /** One pending Activity-side effect (share sheet, calendar, permission screen); see [consumeEffect]. */
    var effect by mutableStateOf<PlannerEffect?>(null); private set
    fun consumeEffect(shown: PlannerEffect) { if (effect == shown) effect = null }

    // ---- Voice: speak to the assistant and hear its replies ----
    internal var voiceFactory: (Application) -> VoiceController = { VoiceController.create(it) }
    private var voiceController: VoiceController? = null
    /** Created on first use (assistant opened); the main thread owns it. */
    val voice: VoiceController
        get() = voiceController ?: voiceFactory(getApplication()).also { created ->
            voiceController = created
            created.setRate(state.voice.rate)
            viewModelScope.launch { created.state.collect(::onVoice) }
            // Read new replies and pending proposals aloud while the assistant is open.
            viewModelScope.launch {
                snapshotFlow { assistantHistory.lastOrNull() to assistantProposal }.collect { (last, proposal) -> speakNew(last, proposal) }
            }
        }
    /** Shown under the microphone: what went wrong while listening, or what to say next. */
    var voiceNotice by mutableStateOf<String?>(null); private set
    private var spokenMessageId: String? = null
    private var spokenProposal: AssistantProposal? = null

    fun startListening() {
        if (!assistantVisible || assistantBusy) return
        voiceNotice = null
        voice.start()
    }
    fun stopListening() { voiceController?.stop() }
    fun stopSpeaking() { voiceController?.stopSpeaking() }
    /** "Đọc lại": reads the latest reply even when automatic reading is off. */
    fun readAgain() {
        val last = assistantHistory.lastOrNull { it.role == "assistant" } ?: return
        voice.speak(AssistantVoice.spoken(last.text))
    }

    private fun onVoice(heard: VoiceState) {
        val address = state.voice.addressAs
        if (cookAlong != null) { onCookVoice(heard); return }
        when (heard) {
            is VoiceState.Result -> {
                voice.reset()
                when (val intent = AssistantVoice.route(heard.text, assistantProposal != null)) {
                    HeardIntent.Confirm -> { assistantHistory = assistantHistory + AssistantChatMessage("user", heard.text); confirmAssistantProposal() }
                    HeardIntent.Cancel -> { assistantHistory = assistantHistory + AssistantChatMessage("user", heard.text); cancelAssistantProposal() }
                    HeardIntent.AskAgain -> { voiceNotice = AssistantVoice.askAgain(address); say(voiceNotice.orEmpty()) }
                    is HeardIntent.Send -> if (intent.text.isNotBlank()) askAssistant(intent.text)
                }
            }
            is VoiceState.Error -> {
                voice.reset()
                voiceNotice = AssistantVoice.error(heard.kind, address)
                say(voiceNotice.orEmpty())
            }
            else -> Unit
        }
    }

    private fun speakNew(last: AssistantChatMessage?, proposal: AssistantProposal?) {
        val parts = mutableListOf<String>()
        if (last != null && last.role == "assistant" && last.id != spokenMessageId) { spokenMessageId = last.id; parts += last.text }
        if (proposal != null && proposal !== spokenProposal) {
            spokenProposal = proposal
            val meals = proposal.actions.filter { it.type == AssistantActionType.swap_recipe }.map { it.date to it.meal }.distinct().size
            parts += AssistantVoice.proposalPrompt(proposal.descriptions, meals, state.voice.addressAs)
        }
        if (parts.isNotEmpty()) say(parts.joinToString(". "))
    }

    private fun say(text: String) {
        if (assistantVisible && state.voice.readAloud && text.isNotBlank()) voiceController?.speak(AssistantVoice.spoken(text))
    }

    fun saveDiet(diet: DietProfile) {
        val known = engine.recipes.values.flatMap { it.ingredients }.map { it.name }.toSet()
        update(state.copy(diet = diet.copy(tags = diet.tags intersect Recipe.TAGS, allergens = diet.allergens intersect Recipe.ALLERGENS,
            avoid = diet.avoid intersect known)))
        message = "Đã lưu hồ sơ ăn uống. Gợi ý và thực đơn mới sẽ theo hồ sơ này."
    }
    /** A typed term as an allergen code ("tôm" → shrimp) or canonical ingredient names; null when unknown. */
    fun resolveDietTerm(term: String): Pair<String?, Set<String>>? = runCatching { commands.resolveAvoid(term.trim()) }.getOrNull()
    /** How many planned meals this week contain a dish that does not fit [diet]. */
    fun mealsOutsideDiet(diet: DietProfile = state.diet): Int = days.sumOf { day ->
        Meal.entries.count { meal -> engine.menu(state, day, meal).any { !diet.allows(it) } }
    }

    fun saveVoiceSettings(addressAs: String, readAloud: Boolean, rate: Float, liveVoice: String = state.voice.liveVoice) {
        val settings = VoiceSettings(VoiceSettings.clean(addressAs), readAloud, rate.coerceIn(VoiceController.MIN_RATE, VoiceController.MAX_RATE),
            VoiceSettings.liveVoice(liveVoice))
        update(state.copy(voice = settings))
        voiceController?.setRate(settings.rate)
        if (!readAloud) stopSpeaking()
        message = "Đã lưu cách trợ lý nói chuyện."
    }
    /** "Nghe thử" on the Family screen, at the chosen speed. */
    fun previewVoice(addressAs: String, rate: Float) {
        voice.setRate(rate)
        voice.speak("Chào ${VoiceSettings.clean(addressAs)}, mình là trợ lý Bếp Nhà. Hôm nay mình nấu gì nhé?")
    }

    override fun onCleared() {
        liveController?.stop()
        liveController = null
        voiceController?.release()
        voiceController = null
        super.onCleared()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for ((snapshot, outcome) in updates) {
                try {
                    storage.save(snapshot)
                    ReminderStore.synchronize(application, PlannerStorage.alarmJson(engine.alarms(snapshot, System.currentTimeMillis())))
                    if (outcome != null) withContext(Dispatchers.Main) { assistantHistory = assistantHistory + AssistantChatMessage("assistant", outcome, source = AssistantSource.SYSTEM) }
                } catch (exception: Exception) {
                    withContext(Dispatchers.Main) {
                        message = "Chưa lưu được dữ liệu hoặc lịch nhắc. Bạn hãy mở lại ứng dụng để kiểm tra."
                        if (outcome != null) assistantError = message
                    }
                }
            }
        }
        updates.trySend(state to null)
        viewModelScope.launch(Dispatchers.IO) {
            for (messages in chatSaves) runCatching { chatStore.save(messages) }
        }
    }

    /** "Xóa lịch sử trò chuyện": clears the chat on screen and on the device. Saved meal data is untouched. */
    fun clearAssistantHistory() {
        if (assistantBusy || assistantProposal != null) return
        revisingProposal = null
        offeredMeals = null
        failedTurn = null
        failedMessageId = null
        assistantError = null
        unansweredMessageIds.clear()
        lastMenuChanges = emptyList()
        greetedFor = null
        assistantHistory = emptyList()
        voiceController?.stopSpeaking()
        message = "Đã xóa lịch sử trò chuyện."
    }

    val menu by derivedStateOf { engine.menu(state) }
    val tasks by derivedStateOf { engine.timeline(state) }
    val days by derivedStateOf { (0L..6L).map(today::plusDays) }
    val shoppingMenu by derivedStateOf { engine.shoppingMenu(state) }
    val fridgeTrays by derivedStateOf { engine.suggestTrays(fridgeItems, fridgeMeal, diet = state.diet, people = state.profile.people) }
    val reminderEnabled get() = state.key in state.reminders

    private fun update(next: PlannerState, outcome: String? = null) {
        if (next != state) {
            if (assistantBusy || assistantProposal != null || revisingProposal != null || failedTurn != null) {
                invalidateTurn("Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn.")
            }
            assistantProposal = null
            revisingProposal = null
            offeredMeals = null
            lastMenuChanges = emptyList()
        }
        state = next
        updates.trySend(next to outcome)
    }
    fun navigate(next: Screen) {
        if (assistantVisible) finishAssistant(restoreRecipe = false)
        screen = next
    }
    fun selectDate(date: LocalDate) { if (date in days) update(state.copy(date = date)) }
    fun selectMeal(meal: Meal) { update(state.copy(meal = meal)) }
    fun openRecipe(id: String) { if (id in engine.recipes) presentSheet(PlannerSheet.RecipeDetail(id)) }
    fun openSwap(index: Int) { presentSheet(PlannerSheet.Swap(index)) }
    fun openReminders() { presentSheet(PlannerSheet.Reminder) }
    private fun presentSheet(content: PlannerSheet) {
        sheetVersion++
        sheet = content
    }
    fun openAsk(tab: AskTab) { askTab = tab; screen = Screen.ASK }
    fun selectAskTab(tab: AskTab) { if (tab == AskTab.CHAT) openAssistant() else askTab = tab }
    fun openAssistant() {
        if (assistantVisible) {
            (sheet as? PlannerSheet.RecipeDetail)?.let { recipe ->
                if (assistantRecipeId != recipe.id) {
                    if (assistantBusy || assistantProposal != null || revisingProposal != null || failedTurn != null) {
                        invalidateTurn("Món đang hỏi đã thay đổi. Hãy gửi lại câu hỏi.")
                    }
                    assistantProposal = null
                    revisingProposal = null
                    offeredMeals = null
                }
                assistantRecipeId = recipe.id
                returnRecipeId = recipe.id
                sheet = null
            }
            return
        }
        returnRecipeId = (sheet as? PlannerSheet.RecipeDetail)?.id
        assistantRecipeId = returnRecipeId
        assistantOrigin = screen
        sheet = null
        assistantVisible = true
        voiceNotice = null
        // Earlier replies were heard (or skipped) already; only read what arrives from now on.
        spokenMessageId = assistantHistory.lastOrNull()?.id
        spokenProposal = assistantProposal
        greet()
        if (state.voice.readAloud) voice.setRate(state.voice.rate)
    }

    private var greetedFor: String? = null
    /** A local summary of the next meal, once per meal, while no turn is open. Read aloud like any reply. */
    private fun greet() {
        if (assistantBusy || assistantProposal != null) return
        val next = AssistantGreeting.upcoming(state, today, System.currentTimeMillis())
        val key = next?.let { PlannerEngine.key(it.date, it.meal) } ?: "none"
        if (greetedFor == key) return
        greetedFor = key
        assistantHistory = assistantHistory + AssistantChatMessage("assistant",
            AssistantGreeting.text(engine, state, today, System.currentTimeMillis()),
            next?.let { engine.menu(state, it.date, it.meal).map { recipe -> recipe.id } }.orEmpty(),
            id = "greeting-${java.util.UUID.randomUUID()}", source = AssistantSource.SYSTEM)
    }

    // ---- "Nấu cùng tôi": one step per screen, read aloud, voice commands ----
    var cookAlong by mutableStateOf<CookAlong?>(null); private set

    fun startCookAlong(date: LocalDate = state.date, meal: Meal = state.meal) {
        if (date !in days) return
        if (assistantVisible) finishAssistant(restoreRecipe = false)
        sheet = null
        if (state.date != date || state.meal != meal) update(state.copy(date = date, meal = meal))
        val steps = CookAlongPlan.steps(engine, state, date, meal)
        if (steps.isEmpty()) return
        // Resume at the first step of the first task not done yet.
        val done = state.done[PlannerEngine.key(date, meal)].orEmpty()
        val start = steps.indexOfFirst { it.taskKey !in done }.takeIf { it >= 0 } ?: 0
        cookAlong = CookAlong(date, meal, steps, start)
        voiceNotice = null
        liveUnavailable = false
        cancelFinishing()
        readStep()
    }
    fun cookNext() = advance(announce = true)
    fun cookBack() = goBack(announce = true)
    fun readStep() {
        val session = cookAlong ?: return
        val spoken = CookAlongPlan.spoken(session)
        if (liveCookOpen) liveCook.say(CookLivePrompt.readStep(AssistantVoice.spoken(spoken))) else voice.speak(AssistantVoice.spoken(spoken))
    }
    fun cookTimer(minutes: Int) { setCookTimer(minutes, announce = true) }
    fun exitCookAlong() {
        cookAlong = null
        cancelFinishing()
        liveController?.stop()
        voiceController?.reset()
        voiceController?.stopSpeaking()
        voiceNotice = null
    }
    /** The microphone button: opens or closes a Gemini Live conversation, or listens for one short command. */
    fun startCookListening() {
        if (cookAlong == null) return
        voiceNotice = null
        if (!cookUsesLive) { voice.start(); return }
        if (liveCookOpen) { stopCookLive(); return }
        voiceController?.reset()
        voiceController?.stopSpeaking()
        liveCook.start(::liveSetup, CookLivePrompt.greeting())
    }
    fun stopCookLive() { liveController?.stop() }

    private fun advance(announce: Boolean) {
        val session = cookAlong ?: return
        if (session.finishesTask) markTaskDone(session.date, session.meal, session.step.taskKey)
        if (session.isLast) {
            exitCookAlong()
            message = "Xong rồi! Chúc cả nhà ngon miệng."
            voice.speak("Xong rồi! Chúc cả nhà ngon miệng.")
            return
        }
        cookAlong = session.copy(index = session.index + 1)
        if (announce) readStep()
    }
    private fun goBack(announce: Boolean) {
        val session = cookAlong ?: return
        // Going back undoes a voice "finished": the cook-along stays open and Gemini keeps working.
        cancelFinishing()
        if (session.index > 0) cookAlong = session.copy(index = session.index - 1)
        if (announce) readStep()
    }
    /** Starts a timer for the current step; returns whether it started and what to tell the user. */
    private fun setCookTimer(minutes: Int, announce: Boolean): Pair<Boolean, String>? {
        val session = cookAlong ?: return null
        if (minutes !in 1..180) return null
        val before = state.timers
        startTimer(minutes, "${session.step.title}: bước ${session.index + 1}")
        val said = message.orEmpty()
        if (announce && said.isNotEmpty()) { if (liveCookOpen) liveCook.say(CookLivePrompt.announce(said)) else voice.speak(said) }
        return (state.timers != before) to said
    }

    private fun onCookVoice(heard: VoiceState) {
        when (heard) {
            is VoiceState.Result -> {
                voice.reset()
                when (val command = VoiceCommand.parse(heard.text)) {
                    CookCommand.Next -> cookNext()
                    CookCommand.Back, CookCommand.Undo -> cookBack()
                    CookCommand.Repeat -> readStep()
                    is CookCommand.Timer -> cookTimer(command.minutes)
                    CookCommand.Unknown -> {
                        voiceNotice = "Nói “tiếp”, “đọc lại”, “quay lại” hoặc “hẹn giờ 10 phút” nhé."
                        voice.speak(voiceNotice.orEmpty())
                    }
                }
            }
            is VoiceState.Error -> {
                voice.reset()
                voiceNotice = AssistantVoice.cookError(heard.kind, state.voice.addressAs)
                voice.speak(voiceNotice.orEmpty())
            }
            else -> Unit
        }
    }

    // ---- Gemini Live inside "Nấu cùng tôi" (the chat assistant keeps its own model) ----
    internal var liveCookFactory: (Application) -> LiveCookEngine = { FirebaseLiveCookEngine(it) }
    private var liveController: LiveCookController? = null
    private val liveCook: LiveCookController
        get() = liveController ?: LiveCookController(liveCookFactory(getApplication()), viewModelScope).also { created ->
            liveController = created
            created.onCall = ::onLiveCall
            viewModelScope.launch { created.state.collect(::onLiveState) }
        }
    var liveCookState by mutableStateOf<LiveCookState>(LiveCookState.Off); private set
    val liveCookOpen get() = liveCookState.let { it is LiveCookState.Connecting || it is LiveCookState.Active }
    /** Set after a lasting Live failure (quota, configuration); the rest of this cook-along uses on-device commands. */
    private var liveUnavailable by mutableStateOf(false)
    /** Same rule as the chat assistant; tests may turn it on to drive a fake engine. */
    internal var cookLiveAllowed by mutableStateOf(assistantConfigured)
    /** Whether the microphone talks to Gemini Live rather than parsing short commands on the device. */
    val cookUsesLive get() = cookLiveAllowed && !liveUnavailable
    /** The cook-along Gemini just finished by voice; the screen closes once Gemini has said goodbye. */
    private var finishing: CookAlong? = null
    private var finishJob: Job? = null

    private fun cancelFinishing() {
        finishJob?.cancel()
        finishJob = null
        finishing = null
    }

    private fun liveSetup(): LiveCookSetup? = cookAlong?.let { session ->
        LiveCookSetup(CookLivePrompt.instruction(engine, state, session), state.voice.liveVoice)
    }

    private fun onLiveState(live: LiveCookState) {
        liveCookState = live
        if (live !is LiveCookState.Failed || cookAlong == null) return
        if (BuildConfig.DEBUG) android.util.Log.w("BepNhaLive", "Live failed: ${live.kind}")
        // A dropped connection may be retried with the next tap; quota or configuration problems will not clear up.
        val retry = CookLivePrompt.retryable(live.kind)
        if (!retry) liveUnavailable = true
        // Say why, and let the user decide: no automatic listening (it would beep and fail again offline).
        voiceNotice = CookLivePrompt.failure(live.kind, retry, state.voice.addressAs)
        voice.speak(voiceNotice.orEmpty())
    }

    /** Runs Gemini's function calls on the main thread; the result is what Gemini reads next. */
    private fun onLiveCall(call: LiveCall): JSONObject {
        val session = cookAlong ?: return JSONObject().put("ok", false).put("error", "Đã thoát chế độ nấu.")
        if (finishing != null) return JSONObject().put("ok", false).put("error", "Bữa này đã nấu xong.")
        fun step() = AssistantVoice.spoken(CookAlongPlan.spoken(cookAlong ?: session))
        return when (val command = CookLiveTools.parse(call)) {
            CookLiveTools.Command.Next -> if (session.isLast) {
                finishing = session
                markTaskDone(session.date, session.meal, session.step.taskKey)
                // Let Gemini say goodbye before the cook screen closes; going back cancels this.
                finishJob = viewModelScope.launch {
                    delay(FINISH_DELAY_MS)
                    if (cookAlong == session && finishing == session) {
                        exitCookAlong()
                        message = "Xong rồi! Chúc cả nhà ngon miệng."
                    }
                }
                JSONObject().put("ok", true).put("done", true).put("say", "Xong rồi! Chúc cả nhà ngon miệng.")
            } else {
                advance(announce = false)
                JSONObject().put("ok", true).put("step", step())
            }
            CookLiveTools.Command.Back -> if (session.index == 0) {
                JSONObject().put("ok", false).put("say", "Đây là bước đầu tiên rồi.")
            } else {
                goBack(announce = false)
                JSONObject().put("ok", true).put("step", step())
            }
            CookLiveTools.Command.Repeat -> JSONObject().put("ok", true).put("step", step())
            is CookLiveTools.Command.Timer -> {
                val (started, said) = setCookTimer(command.minutes, announce = false) ?: (false to "Chỉ hẹn được từ 1 đến 180 phút.")
                JSONObject().put("ok", started).put("say", said)
            }
            CookLiveTools.Command.CancelTimer -> {
                val timer = state.timers.filter { it.date == session.date && it.meal == session.meal }.maxByOrNull { it.id }
                if (timer == null) JSONObject().put("ok", false).put("say", "Chưa có đồng hồ hẹn giờ nào đang chạy.")
                else { cancelTimer(timer.id); JSONObject().put("ok", true).put("say", message.orEmpty()) }
            }
            is CookLiveTools.Command.Invalid -> JSONObject().put("ok", false).put("error", command.reason)
        }
    }

    private companion object {
        /** Long enough for Gemini to say its closing sentence. */
        const val FINISH_DELAY_MS = 6_000L
    }

    private fun markTaskDone(date: LocalDate, meal: Meal, task: String) {
        val key = PlannerEngine.key(date, meal)
        val done = state.done[key].orEmpty()
        if (task !in done && engine.timeline(state, date, meal).any { it.key == task }) update(state.copy(done = state.done + (key to done + task)))
    }
    fun closeAssistant() = finishAssistant(restoreRecipe = true)
    private fun finishAssistant(restoreRecipe: Boolean) {
        if (assistantBusy || (revisingProposal != null && failedTurn != null)) {
            invalidateTurn("Yêu cầu trước đã dừng. Bạn có thể gửi lại câu hỏi.")
        }
        assistantProposal = null
        revisingProposal = null
        offeredMeals = null
        assistantVisible = false
        voiceController?.reset()
        voiceController?.stopSpeaking()
        if (restoreRecipe) returnRecipeId?.let(::openRecipe)
        returnRecipeId = null
    }
    private fun invalidateTurn(reason: String) {
        val prompt = activeTurn?.prompt ?: failedTurn?.prompt ?: assistantProposal?.let { assistantHistory.lastOrNull { it.role == "user" }?.text }
        if (assistantQuestion.isBlank() && prompt != null) assistantQuestion = prompt
        generation++
        activeJob?.cancel()
        activeJob = null
        activeTurn = null
        failedTurn = null
        failedMessageId = null
        assistantBusy = false
        assistantError = reason
    }
    fun cancelAssistantProposal() {
        if (assistantProposal == null && revisingProposal == null) return
        if (assistantBusy || failedTurn != null) invalidateTurn("Đã hủy đề xuất.")
        assistantProposal = null
        revisingProposal = null
        assistantError = null
        assistantHistory = assistantHistory + AssistantChatMessage("assistant", "Đã hủy đề xuất. Dữ liệu được giữ nguyên.", source = AssistantSource.SYSTEM)
    }

    fun editAssistantProposal() {
        val proposal = assistantProposal?.takeIf { it.canReviseMenu } ?: return
        revisingProposal = proposal
        assistantProposal = null
    }

    fun chooseAssistantMeal(id: Int) = chooseAssistantMeal(id, "Chọn mâm $id")

    private fun chooseAssistantMeal(id: Int, prompt: String) {
        if (!assistantVisible || assistantBusy || assistantProposal != null) return
        val suggestion = offeredMeals?.takeIf { it.base == state }?.options?.firstOrNull { it.id == id }
        val reply = if (suggestion == null) {
            "Bạn hãy yêu cầu gợi ý mâm cho bữa muốn nấu trước khi chọn."
        } else {
            val proposal = commands.proposal(state, commands.actionsFor(state, suggestion, today), today, now)
            offeredMeals = null
            revisingProposal = null
            assistantProposal = proposal.takeIf { it.actions.isNotEmpty() }
            if (assistantProposal == null) "Mâm này đang có trong thực đơn." else "Bạn xem lại các món sẽ đổi trong mâm $id."
        }
        assistantHistory = assistantHistory + AssistantChatMessage("user", prompt) +
            AssistantChatMessage("assistant", reply, source = AssistantSource.SYSTEM)
        assistantQuestion = ""
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }

    fun submitAssistant() {
        askAssistant()
    }
    fun askAssistant(question: String = assistantQuestion) {
        if (question.isBlank() || assistantBusy || assistantProposal != null || !assistantVisible) return
        val prompt = question.trim().take(1000)
        if (undoPhrase.matches(PlannerEngine.normalize(prompt).trimEnd('.', '?', '!'))) {
            assistantHistory = assistantHistory + AssistantChatMessage("user", prompt)
            assistantQuestion = ""
            undoAssistant()
            return
        }
        val choice = mealChoice.matchEntire(PlannerEngine.normalize(prompt).trimEnd('.', '?', '!'))
        if (choice != null) {
            val option = choice.groupValues[1]
            chooseAssistantMeal(if (option in setOf("1", "mot", "thu nhat")) 1 else 2, prompt)
            return
        }
        // A failed message is not supplied as an already answered history turn.
        // Local greetings are not part of the conversation the model needs.
        val recent = System.currentTimeMillis() - ChatStore.MODEL_WINDOW_MS
        val history = assistantHistory.filterNot { it.id in unansweredMessageIds || it.id.startsWith("greeting-") || it.at < recent }
        val user = AssistantChatMessage("user", prompt)
        unansweredMessageIds += user.id
        assistantHistory = assistantHistory + user
        assistantQuestion = ""
        val comparing = MealPlanning.isComparison(prompt)
        val turn = Turn(prompt, history, state, today, assistantOrigin, assistantRecipeId,
            revisingProposal?.actions.orEmpty(), lastMenuChanges,
            offeredMeals?.takeIf { it.base == state }, comparing, user.id)
        if (!comparing) offeredMeals = null
        startTurn(turn)
    }

    private fun turnContext(turn: Turn): org.json.JSONObject {
        val context = AssistantGateway.snapshot(engine, turn.base, turn.today, turn.origin, turn.recipeId,
            pendingMenuChanges = turn.pendingChanges, lastMenuChanges = turn.lastChanges)
        val suggestions = turn.suggestions
        suggestions?.options?.firstOrNull()?.let {
            context.put("suggested_meal", org.json.JSONObject().put("date", it.date.toString()).put("meal", it.meal.value))
        }
        suggestions?.constraints?.let {
            context.put("suggestion_constraints", org.json.JSONObject()
                .put("pantry", org.json.JSONArray(it.pantry.toList())).put("excluded", org.json.JSONArray(it.excluded.toList())).put("only", it.only))
        }
        if (turn.comparing) {
            suggestions?.let { batch ->
                context.put("suggestion_turn_id", batch.requestId)
                context.put("active_suggestions", org.json.JSONArray(batch.options.map { option ->
                    org.json.JSONObject().put("id", option.id).put("date", option.date.toString()).put("meal", option.meal.value)
                        .put("recipe_ids", org.json.JSONArray(option.recipeIds))
                }))
            }
        }
        return context
    }
    fun retryAssistant() {
        val turn = failedTurn ?: return
        if (assistantBusy || assistantProposal != null || !assistantVisible) return
        if (turn.base != state) {
            invalidateTurn("Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn.")
            return
        }
        startTurn(turn)
    }
    private fun startTurn(turn: Turn) {
        val token = ++generation
        activeTurn = turn
        failedTurn = null
        failedMessageId = null
        assistantBusy = true
        assistantError = null
        activeJob = viewModelScope.launch {
            try {
                val transport = assistantTransport
                val reply = withContext(Dispatchers.IO) {
                    val context = turnContext(turn)
                    if (transport == null) assistantService.ask(turn.prompt, turn.history, context, turn.base)
                    else transport(turn.prompt, turn.history, context)
                }
                if (token != generation || !assistantVisible) return@launch
                require(state == turn.base) { "Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn." }
                val actions = revisingProposal?.let { commands.reviseMenu(it, reply.actions) } ?: reply.actions
                // Validation messages are written for the model; the user only needs to know the reply was unusable.
                val proposal = try { commands.proposal(state, actions, today, System.currentTimeMillis()) }
                    catch (_: IllegalArgumentException) { throw IllegalStateException(INVALID_REPLY) }
                val responseId = java.util.UUID.randomUUID().toString()
                if (reply.suggestions.isNotEmpty()) {
                    val first = reply.suggestions.first()
                    require(reply.actions.isEmpty() && reply.suggestions.size <= 2)
                    reply.suggestions.forEachIndexed { index, suggestion ->
                        require(suggestion.id == index + 1 && suggestion.date == first.date && suggestion.meal == first.meal)
                        commands.actionsFor(state, suggestion, today)
                    }
                    offeredMeals = MealSuggestions(state, reply.suggestions, responseId, turn.messageId, reply.suggestionConstraints)
                    revisingProposal = null
                }
                val replyText = if (actions.isNotEmpty() && proposal.actions.isEmpty()) {
                    revisingProposal = null
                    "Các món đã khớp với thực đơn đang dùng."
                } else reply.text
                unansweredMessageIds -= turn.messageId
                assistantHistory = assistantHistory + AssistantChatMessage("assistant", replyText.take(8000),
                    (reply.recipeIds + proposal.actions.filter { it.type == AssistantActionType.open_recipe }.mapNotNull { it.recipeId }).distinct(),
                    id = responseId, source = reply.source, notice = reply.notice)
                if (proposal.needsConfirmation) {
                    offeredMeals = null
                    revisingProposal = null
                    assistantProposal = proposal
                } else {
                    // READ and LOW actions run now (LOW can be undone). The response is complete
                    // before navigation/actions can invalidate the turn.
                    activeTurn = null
                    assistantBusy = false
                    if (proposal.actions.any { it.mutates }) revisingProposal = null
                    executeAssistantActions(proposal, confirmed = false)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (token == generation) {
                    failedTurn = turn
                    failedMessageId = turn.messageId
                    assistantError = exception.message?.takeUnless {
                        it in setOf("Failed requirement.", "Required value was null.", "Check failed.")
                    } ?: INVALID_REPLY
                }
            } finally {
                if (token == generation) {
                    assistantBusy = false
                    activeTurn = null
                    activeJob = null
                }
            }
        }
    }
    fun lookupOffline() = lookupOnDevice(assistantQuestion)
    fun lookupFailedOffline() {
        val turn = failedTurn ?: return
        if (assistantBusy || assistantProposal != null) return
        val answer = localAnswer(turn.prompt)
        unansweredMessageIds -= turn.messageId
        assistantHistory = assistantHistory + AssistantChatMessage("assistant", answer.text, answer.recipes, source = AssistantSource.LOCAL)
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }
    private fun lookupOnDevice(question: String) {
        if (question.isBlank() || assistantBusy || assistantProposal != null) return
        val prompt = question.trim().take(1000)
        val answer = localAnswer(prompt)
        assistantHistory = assistantHistory + AssistantChatMessage("user", prompt) +
            AssistantChatMessage("assistant", answer.text, answer.recipes, source = AssistantSource.LOCAL)
        assistantQuestion = ""
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }
    private fun localAnswer(prompt: String): AssistantAnswer {
        val recipe = assistantRecipeId?.let(engine.recipes::get)
        val query = PlannerEngine.normalize(prompt)
        if (recipe != null && query.contains("mon nay")) {
            val text = if (query.contains("nguyen lieu")) {
                recipe.ingredients.joinToString("\n") {
                    "${it.name}: ${PlannerEngine.quantity(it.amount * state.profile.people / 4.0)} ${it.unit}"
                }
            } else {
                recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n\n")
            }
            return AssistantAnswer("${recipe.name}\n\n$text", listOf(recipe.id))
        }
        return engine.answer(prompt, state)
    }
    fun confirmAssistantProposal() {
        val proposal = assistantProposal ?: return
        assistantProposal = null
        try {
            require(state == proposal.base) { "Dữ liệu đã thay đổi. Hãy gửi lại câu hỏi để xem đề xuất mới." }
            executeAssistantActions(proposal, confirmed = true)
            if (proposal.canReviseMenu) lastMenuChanges = proposal.actions
        } catch (exception: Exception) {
            if (assistantQuestion.isBlank()) assistantQuestion = assistantHistory.lastOrNull { it.role == "user" }?.text.orEmpty()
            assistantError = exception.message ?: "Không thể thực hiện thao tác."
        }
    }

    /** Remove one meal's dish swaps from the open proposal (the user keeps that meal as it is). */
    fun dropProposalMeal(date: LocalDate, meal: Meal) {
        val proposal = assistantProposal ?: return
        val remaining = proposal.actions.filterNot { it.type == AssistantActionType.swap_recipe && it.date == date && it.meal == meal }
        if (remaining.isEmpty()) { cancelAssistantProposal(); return }
        assistantProposal = commands.proposal(proposal.base, remaining, today, System.currentTimeMillis())
    }

    /** Dish swaps of the open proposal grouped by meal, in day order, for a short readable preview. */
    fun proposalMeals(): List<ProposalMealUi> {
        val proposal = assistantProposal ?: return emptyList()
        return proposal.actions.filter { it.type == AssistantActionType.swap_recipe }
            .groupBy { it.date to it.meal }.toSortedMap(compareBy({ it.first }, { it.second.ordinal }))
            .map { (target, swaps) ->
                ProposalMealUi(target.first, target.second, "${target.second.title} · ${PlannerEngine.fullDate(target.first)}",
                    swaps.sortedBy { it.index }.map { commands.swapLine(proposal.base, it) })
            }
    }
    /** Descriptions of the non-swap actions in the open proposal. */
    fun proposalOther(): List<String> {
        val proposal = assistantProposal ?: return emptyList()
        return proposal.actions.zip(proposal.descriptions).filter { it.first.type != AssistantActionType.swap_recipe }.map { it.second }
    }

    fun undoAssistant() {
        val change = undo.pop(state)
        if (change == null) {
            assistantHistory = assistantHistory + AssistantChatMessage("assistant",
                "Không còn thay đổi nào của trợ lý để hoàn tác, hoặc dữ liệu đã được chỉnh tay sau đó.", source = AssistantSource.SYSTEM)
            return
        }
        update(change.before, "Đã hoàn tác: ${change.label}")
    }

    private fun executeAssistantActions(proposal: AssistantProposal, confirmed: Boolean) {
        val actions = proposal.actions
        val base = proposal.base
        var next = base
        actions.forEach { next = commands.apply(next, it, today, System.currentTimeMillis()) }
        if (actions.any { it.mutates } && next != base) {
            val label = actions.zip(proposal.descriptions).filter { it.first.mutates }
                .joinToString("; ") { it.second.substringAfter('\n').trimEnd('.') }
            val permissions = if ((!notifications || !exactAlarms) && actions.any { it.type in setOf(AssistantActionType.set_reminder, AssistantActionType.start_timer) })
                " Hãy mở Nhắc nấu để kiểm tra quyền thông báo và nhắc đúng giờ." else ""
            val outcome = if (confirmed) "Đã lưu và áp dụng các thay đổi đã xác nhận.$permissions Bạn có thể bấm Hoàn tác nếu muốn trả lại."
                else "Đã làm: $label.$permissions Bấm Hoàn tác hoặc nói “hoàn tác” nếu muốn trả lại."
            undo.push(AssistantChange(base, next, label))
            update(next, outcome)
        }
        actions.forEach { action ->
            when (action.type) {
                AssistantActionType.navigate -> {
                    update(state.copy(date = action.date, meal = action.meal)); navigate(requireNotNull(action.screen))
                }
                AssistantActionType.open_recipe -> {
                    update(state.copy(date = action.date, meal = action.meal))
                    openRecipe(requireNotNull(action.recipeId))
                }
                AssistantActionType.select_meal -> if (action.date in days) update(state.copy(date = action.date, meal = action.meal))
                AssistantActionType.open_sheet -> {
                    update(state.copy(date = action.date, meal = action.meal))
                    if (action.option == "reminder") openReminders() else openSwap(requireNotNull(action.index))
                }
                AssistantActionType.close_assistant -> closeAssistant()
                AssistantActionType.share_shopping_list ->
                    effect = PlannerEffect.Share(shoppingText(action.date, if (action.option == "day") engine.meals.toSet() else setOf(action.meal)))
                AssistantActionType.add_to_calendar -> effect = PlannerEffect.Calendar(engine.timeline(state, action.date, action.meal))
                AssistantActionType.request_permission ->
                    effect = if (action.option == "exact_alarms") PlannerEffect.ExactAlarms else PlannerEffect.Notifications
                AssistantActionType.start_cook_along -> startCookAlong(action.date, action.meal)
                else -> Unit
            }
        }
    }
    fun assistantSuggestions(): List<String> = when {
        assistantRecipeId != null -> listOf("Giải thích cách nấu món này", "Nguyên liệu món này gồm gì?")
        assistantOrigin == Screen.SHOPPING -> listOf("Còn thiếu nguyên liệu gì?", "Mở thực đơn bữa này")
        assistantOrigin == Screen.COOK -> listOf("Nấu món nào trước?", "Nhắc mình chuẩn bị bữa này")
        assistantConfigured -> listOf("Lên thực đơn cả tuần, ăn nhẹ và nấu nhanh", "Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?", "Tối mai nấu canh chua cá nhé")
        else -> listOf("Đổi món mặn", "Món nấu nhanh", "Nấu món nào trước?")
    }
    fun dismissSheet() { sheet = null }
    fun clearMessage(shown: String) { if (message == shown) message = null }
    fun showMessage(text: String) { message = text }

    fun swap(index: Int, id: String) {
        update(engine.swap(state, index, id)); sheet = null
        message = "Đã đổi món. Nguyên liệu và lịch nấu đã cập nhật."
    }
    fun toggleFavorite(id: String) {
        val favorites = if (id in state.favorites) state.favorites - id else state.favorites + id
        update(state.copy(favorites = favorites))
    }

    // ---- Shopping ----
    fun toggleShopMeal(meal: Meal) {
        val current = state.shoppingMeals
        val next = if (meal in current) current - meal else current + meal
        if (next.isEmpty()) { message = "Chọn ít nhất một bữa để đi chợ."; return }
        selectShoppingMeals(next)
    }
    fun shopWholeDay() = selectShoppingMeals(engine.meals.toSet())

    private fun selectShoppingMeals(selected: Set<Meal>) {
        if (selected == state.shoppingMeals) return
        val shop = PlannerEngine.shopKey(state.date)
        if (shop !in state.checked) {
            update(state.copy(shopMeals = selected))
            return
        }
        val previous = engine.ingredients(shoppingMenu, state.profile.people).associate { it.key to it.amount }
        val more = engine.ingredients(engine.shoppingMenu(state, meals = selected), state.profile.people)
            .filter { it.amount > (previous[it.key] ?: 0.0) }.map { it.key }.toSet()
        update(state.copy(shopMeals = selected, checked = state.checked + (shop to (checkedIngredients - more))))
    }
    fun checkIngredient(key: String) {
        val shop = PlannerEngine.shopKey(state.date)
        update(state.copy(checked = state.checked + (shop to checkedIngredients.toggle(key))))
    }
    val checkedIngredients: Set<String> get() = engine.checkedForShopping(state, state.date, state.shoppingMeals)

    /** The day's list: ingredients of the chosen meals plus items added by hand; equal items are summed. */
    fun shoppingItems(date: LocalDate = state.date, meals: Set<Meal> = state.shoppingMeals): List<Ingredient> {
        val merged = linkedMapOf<String, Ingredient>()
        (engine.ingredients(engine.shoppingMenu(state, date, meals), state.profile.people) +
            state.extras[PlannerEngine.shopKey(date)].orEmpty()).forEach { item ->
            merged[item.key] = merged[item.key]?.let { it.copy(amount = it.amount + item.amount) } ?: item
        }
        return merged.values.toList()
    }
    fun addShoppingItem(name: String, amount: Double, unit: String, group: String) {
        val clean = name.trim().take(60)
        if (clean.isEmpty() || amount <= 0.0 || amount > 100_000) { message = "Nhập tên và số lượng hợp lệ."; return }
        val shop = PlannerEngine.shopKey(state.date)
        val item = Ingredient(clean.replaceFirstChar { it.titlecase(PlannerEngine.locale) }, amount, unit.trim().take(20), group)
        update(state.copy(extras = state.extras + (shop to (state.extras[shop].orEmpty() + item)),
            checked = state.checked + (shop to (checkedIngredients - item.key))))
        message = "Đã thêm ${item.name} vào danh sách."
    }
    fun removeShoppingExtras(group: String) {
        val shop = PlannerEngine.shopKey(state.date)
        val left = state.extras[shop].orEmpty().filterNot { it.group == group }
        update(state.copy(extras = if (left.isEmpty()) state.extras - shop else state.extras + (shop to left)))
    }
    fun setIngredientsChecked(keys: Collection<String>, done: Boolean) {
        val shop = PlannerEngine.shopKey(state.date)
        update(state.copy(checked = state.checked + (shop to if (done) checkedIngredients + keys else checkedIngredients - keys.toSet())))
    }
    /** Unticks everything and drops hand-added items for the selected day. */
    fun clearShopping() {
        val shop = PlannerEngine.shopKey(state.date)
        update(state.copy(checked = state.checked + (shop to emptySet()), extras = state.extras - shop))
        message = "Đã làm mới danh sách đi chợ."
    }

    fun checkTask(key: String) {
        val done = state.done[state.key].orEmpty().toggle(key)
        update(state.copy(done = state.done + (state.key to done)))
    }
    fun saveFamily(name: String, people: Int, breakfast: LocalTime, lunch: LocalTime, dinner: LocalTime) {
        val changedPeople = people != state.profile.people
        update(state.copy(profile = FamilyProfile(name.trim().take(35).ifBlank { "Nhà mình" }, people.coerceIn(1, 12), lunch, dinner, breakfast),
            checked = if (changedPeople) emptyMap() else state.checked))
        message = "Đã lưu thông tin gia đình và cập nhật định lượng."
    }
    /** Ends the first-run welcome; a family name and size are saved when the family set them up. */
    fun finishWelcome(name: String? = null, people: Int? = null) {
        if (name != null || people != null) {
            val size = (people ?: state.profile.people).coerceIn(1, 12)
            update(state.copy(profile = state.profile.copy(name = name?.trim()?.take(35)?.ifBlank { null } ?: state.profile.name, people = size),
                checked = if (size != state.profile.people) emptyMap() else state.checked))
        }
        storage.markWelcomeSeen()
        showWelcome = false
    }
    fun saveReminder(time: LocalTime, enabled: Boolean) {
        update(state.copy(profile = state.profile.with(state.meal, time),
            reminders = if (enabled) state.reminders + state.key else state.reminders - state.key))
        sheet = null
        message = when {
            !enabled -> "Đã tắt nhắc nấu cho bữa này."
            tasks.first().at <= now -> "Giờ bắt đầu đã qua. Bạn hãy chỉnh giờ ăn nếu nấu muộn hơn."
            !notifications -> "Đã lưu lịch. Hãy cho phép thông báo để nhận nhắc nấu."
            else -> "Đã đặt nhắc vào bếp lúc ${PlannerEngine.time(tasks.first().at)}."
        }
    }
    fun saveShoppingReminder(enabled: Boolean, time: LocalTime) {
        update(state.copy(shopping = ShoppingReminder(enabled, time)))
        message = if (!enabled) "Đã tắt nhắc đi chợ." else if (!notifications) "Đã lưu. Hãy cho phép thông báo để nhận nhắc đi chợ."
            else "Mỗi sáng lúc $time sẽ nhắc những đồ còn thiếu."
    }
    fun startTimer(minutes: Int, label: String) {
        val clock = System.currentTimeMillis()
        val next = try {
            engine.startTimer(state, clock, minutes, label, state.date, state.meal)
        } catch (e: IllegalArgumentException) {
            message = e.message
            return
        }
        update(next)
        now = clock
        sheet = null
        message = "Đã hẹn $minutes phút. Đang chạy ${next.timers.size} đồng hồ."
    }

    fun cancelTimer(id: Long) {
        val timer = state.timers.firstOrNull { it.id == id } ?: return
        update(state.copy(timers = state.timers - timer)); message = "Đã dừng hẹn giờ ${timer.label.lowercase(PlannerEngine.locale)}."
    }
    fun visibleTick() {
        now = System.currentTimeMillis()
        if (PlannerEngine.today() != today) {
            today = PlannerEngine.today(); update(engine.ensureWeek(state, today))
        }
        val finished = state.timers.filter { it.end <= now }
        if (finished.isNotEmpty()) {
            update(state.copy(timers = state.timers - finished.toSet()))
            message = "Hết giờ: ${finished.joinToString { it.label }}. Hãy kiểm tra món ăn nhé!"
        }
    }

    // ---- Hỏi Bếp Nhà ----
    fun toggleFridgeItem(item: String) {
        fridgeItems = if (fridgeItems.any { it.equals(item, ignoreCase = true) })
            fridgeItems.filterNot { it.equals(item, ignoreCase = true) } else fridgeItems + item
    }
    fun addFridgeText(text: String) {
        val items = text.split(Regex(",|;|\\.|\\s+và\\s+|\\s+với\\s+|\\n", RegexOption.IGNORE_CASE))
            .map { it.trim().replaceFirstChar { c -> c.titlecase(PlannerEngine.locale) } }.filter { it.length >= 2 }
        fridgeItems = (fridgeItems + items).distinctBy { PlannerEngine.words(it) }.take(16)
        fridgeDraft = ""
    }
    fun clearFridge() { fridgeItems = emptyList() }
    fun selectFridgeMeal(meal: Meal) { fridgeMeal = meal }
    fun openFridgeWith(item: String) {
        if (fridgeItems.none { it.equals(item, ignoreCase = true) }) fridgeItems = fridgeItems + item
        openAsk(AskTab.FRIDGE)
    }
    fun applyTray(date: LocalDate, meal: Meal, ids: List<String>) {
        if (date !in days) return
        update(engine.setPlan(state, date, meal, ids).copy(date = date, meal = meal))
        message = "Đã chọn ${engine.recipes.getValue(ids[0]).name.lowercase(PlannerEngine.locale)} cho ${meal.title.lowercase(PlannerEngine.locale)} " +
            "${PlannerEngine.dayLabel(date, today)}. Danh sách đi chợ đã cập nhật."
    }
    fun isPlanned(date: LocalDate, meal: Meal, ids: List<String>) = state.plans[PlannerEngine.key(date, meal)]?.containsAll(ids) == true

    fun receiveSpeech(text: String) {
        if (text.isBlank()) return
        addFridgeText(text)
    }

    fun refreshPermissions() {
        notifications = ReminderStore.canNotify(getApplication())
        exactAlarms = ReminderStore.canScheduleExact(getApplication())
        viewModelScope.launch(Dispatchers.IO) { ReminderStore.restore(getApplication()) }
    }
    fun openNotification(intent: Intent) {
        val date = runCatching { LocalDate.parse(intent.getStringExtra("date")) }.getOrNull() ?: return
        val meal = Meal.from(intent.getStringExtra("meal").orEmpty()) ?: return
        notificationVersion++
        if (date in days) update(state.copy(date = date, meal = meal))
        if (assistantVisible) finishAssistant(restoreRecipe = false)
        exitCookAlong()
        sheet = null; screen = if (intent.getStringExtra("screen") == "shopping") Screen.SHOPPING else Screen.COOK
    }
    internal fun backDestination(): PlannerPage? = when {
        sheet != null -> null
        cookAlong != null && assistantVisible -> PlannerPage.Chat
        cookAlong != null || assistantVisible -> PlannerPage.ScreenPage(screen)
        screen != Screen.TODAY -> PlannerPage.ScreenPage(Screen.TODAY)
        else -> null
    }
    fun back(): Boolean {
        if (sheet != null) { sheet = null; return true }
        if (cookAlong != null) { exitCookAlong(); return true }
        if (assistantVisible) { closeAssistant(); return true }
        if (screen != Screen.TODAY) { screen = Screen.TODAY; return true }
        return false
    }
    fun shoppingText(date: LocalDate = state.date, meals: Set<Meal> = state.shoppingMeals): String {
        val title = if (meals.size == Meal.entries.size) "Cả ngày" else meals.joinToString(" + ") { it.title }
        return "$title ${PlannerEngine.shortDate(date)} — ${state.profile.people} người\n" +
            shoppingItems(date, meals).joinToString("\n") { "□ ${it.name}: ${PlannerEngine.quantity(it.amount)} ${it.unit}" }
    }
}

private val mealChoice = Regex("""(?:minh )?(?:chon|lay|dung|ap dung)\s+mam\s+(?:so\s+)?(1|2|mot|hai|thu nhat|thu hai)(?:\s+(?:nhe|di))?""")
private const val INVALID_REPLY = "Phản hồi trợ lý chưa hợp lệ. Hãy thử lại hoặc tìm trong công thức có sẵn."
private val undoPhrase = Regex("""(?:hoan tac|tra lai nhu cu|quay lai nhu cu|undo)(?:\s+(?:di|nhe|giup minh|giup toi|giup chau))?""")

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
