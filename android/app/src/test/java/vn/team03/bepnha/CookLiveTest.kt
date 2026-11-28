package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import vn.team03.bepnha.CookLiveTools.Command
import vn.team03.bepnha.voice.LiveCall

class CookLiveTest {
    private val engine = AssistantTestCatalog.engine
    private val day = AssistantTestCatalog.day
    private val state = AssistantTestCatalog.state()

    private fun parse(name: String, vararg args: Pair<String, String>) = CookLiveTools.parse(LiveCall(name, args.toMap()))

    @Test fun stepFunctionsMapToCommands() {
        assertEquals(Command.Next, parse("next_step"))
        assertEquals(Command.Back, parse("previous_step"))
        assertEquals(Command.Repeat, parse("repeat_step"))
        assertEquals(Command.CancelTimer, parse("cancel_timer"))
        assertTrue(parse("swap_dish") is Command.Invalid)
    }

    @Test fun timerMinutesMustBeAWholeNumberInRange() {
        assertEquals(Command.Timer(10), parse("set_timer", "minutes" to "10"))
        assertEquals(Command.Timer(10), parse("set_timer", "minutes" to "10.0"))
        assertEquals(Command.Timer(180), parse("set_timer", "minutes" to " 180 "))
        listOf("0", "181", "2.5", "mười", "NaN", "").forEach { assertTrue(it, parse("set_timer", "minutes" to it) is Command.Invalid) }
        assertTrue(parse("set_timer") is Command.Invalid)
    }

    @Test fun everyDeclaredFunctionIsUnderstood() {
        CookLiveTools.specs.forEach { spec ->
            val args = if (spec.takesMinutes) arrayOf(CookLiveTools.MINUTES to "5") else emptyArray()
            assertFalse(spec.name, parse(spec.name, *args) is Command.Invalid)
        }
    }

    @Test fun instructionCarriesTheMealStepsAndHowToAddressTheCook() {
        val elder = state.copy(voice = VoiceSettings(addressAs = "bác"))
        val steps = CookAlongPlan.steps(engine, elder, day, Meal.DINNER)
        val session = CookAlong(day, Meal.DINNER, steps, index = 2)
        val text = CookLivePrompt.instruction(engine, elder, session)
        assertTrue(text.contains("tiếng Việt"))
        assertTrue(text.contains("Gọi người nấu là \"bác\", xưng là \"cháu\""))
        assertTrue(text.contains("đang ở bước 3"))
        steps.forEach { assertTrue(it.text, text.contains(it.text)) }
        engine.menu(elder, day, Meal.DINNER).forEach { assertTrue(it.name, text.contains(it.name)) }
        CookLiveTools.specs.forEach { assertTrue(it.name, text.contains(it.name)) }
        assertTrue(text.contains(CookLivePrompt.APP))
    }

    @Test fun instructionCarriesTheFamilyDietSoSubstitutesStaySafe() {
        val careful = state.copy(diet = DietProfile(tags = setOf("low_salt"), allergens = setOf("shrimp", "peanut")))
        val session = CookAlong(day, Meal.DINNER, CookAlongPlan.steps(engine, careful, day, Meal.DINNER))
        val text = CookLivePrompt.instruction(engine, careful, session)
        assertTrue(text, text.contains("Hồ sơ ăn uống của gia đình: ${careful.diet.describe()}."))
        listOf("ít muối", "tôm", "đậu phộng").forEach { assertTrue(it, text.contains(it)) }
        assertTrue(text.contains("tuyệt đối không gợi ý thứ gia đình phải tránh"))

        val open = CookLivePrompt.instruction(engine, state, session)
        assertTrue(open.contains("Hồ sơ ăn uống của gia đình: chưa khai báo dị ứng hay kiêng khem."))
    }

    @Test fun liveFailuresSayWhyAndWhetherTheMicrophoneTriesLiveAgain() {
        val transient = listOf(AssistantFailureKind.NETWORK, AssistantFailureKind.TIMEOUT, AssistantFailureKind.UNAVAILABLE)
        val lasting = listOf(AssistantFailureKind.QUOTA, AssistantFailureKind.CONFIGURATION, AssistantFailureKind.UNCONFIGURED)
        transient.forEach { assertTrue(it.name, CookLivePrompt.retryable(it)) }
        lasting.forEach { assertFalse(it.name, CookLivePrompt.retryable(it)) }
        assertTrue(CookLivePrompt.failure(AssistantFailureKind.NETWORK, true, "bác").contains("mất kết nối"))
        assertTrue(CookLivePrompt.failure(AssistantFailureKind.NETWORK, true, "bác").contains("Bác bấm nút micro để thử lại"))
        assertTrue(CookLivePrompt.failure(AssistantFailureKind.QUOTA, false, "bác").contains("hết lượt"))
        assertTrue(CookLivePrompt.failure(AssistantFailureKind.CONFIGURATION, false, "bác").contains("chưa được xác thực"))
        assertTrue(CookLivePrompt.failure(AssistantFailureKind.QUOTA, false, "bác").contains("“tiếp”"))
        AssistantFailureKind.entries.forEach { kind ->
            assertFalse(kind.name, CookLivePrompt.failure(kind, CookLivePrompt.retryable(kind), "bạn").contains("gõ"))
        }
    }

    @Test fun cookScreenVoiceErrorsNeverOfferTyping() {
        vn.team03.bepnha.voice.VoiceErrorKind.entries.filter { it != vn.team03.bepnha.voice.VoiceErrorKind.NoPermission }.forEach { kind ->
            val text = AssistantVoice.cookError(kind, "bác")
            assertFalse(text, text.contains("gõ"))
            assertTrue(text, text.contains("Bác"))
        }
    }

    @Test fun appNotesAreMarkedAsComingFromTheApp() {
        listOf(CookLivePrompt.greeting(), CookLivePrompt.readStep("Bước 1."), CookLivePrompt.announce("Đã hẹn 5 phút."))
            .forEach { assertTrue(it, it.startsWith(CookLivePrompt.APP)) }
        assertTrue(CookLivePrompt.readStep("Bước 1. Rửa rau.").endsWith("Bước 1. Rửa rau."))
    }

    @Test fun liveVoiceFallsBackToTheDefault() {
        assertEquals("Puck", VoiceSettings.liveVoice("Puck"))
        assertEquals(VoiceSettings.DEFAULT_LIVE_VOICE, VoiceSettings.liveVoice("Unknown"))
        assertEquals(VoiceSettings.DEFAULT_LIVE_VOICE, VoiceSettings.liveVoice(null))
        assertEquals(VoiceSettings.DEFAULT_LIVE_VOICE, VoiceSettings().liveVoice)
    }
}
