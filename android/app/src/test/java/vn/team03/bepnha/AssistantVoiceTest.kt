package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import vn.team03.bepnha.voice.VoiceErrorKind

class AssistantVoiceTest {
    @Test fun withoutAProposalEverythingHeardIsSentAsAQuestion() {
        assertEquals(HeardIntent.Send("đồng ý"), AssistantVoice.route(" đồng ý ", proposalPending = false))
        assertEquals(HeardIntent.Send("tối mai nấu canh chua cá"), AssistantVoice.route("tối mai nấu canh chua cá", proposalPending = false))
    }

    @Test fun aPendingProposalOnlyAcceptsAClearYesOrNo() {
        assertEquals(HeardIntent.Confirm, AssistantVoice.route("Đồng ý ạ", proposalPending = true))
        assertEquals(HeardIntent.Confirm, AssistantVoice.route("ừ làm đi", proposalPending = true))
        assertEquals(HeardIntent.Cancel, AssistantVoice.route("thôi", proposalPending = true))
        assertEquals(HeardIntent.Cancel, AssistantVoice.route("không đồng ý", proposalPending = true))
        assertEquals(HeardIntent.AskAgain, AssistantVoice.route("đổi canh khác đi", proposalPending = true))
        assertEquals(HeardIntent.AskAgain, AssistantVoice.route("được nhưng mà không", proposalPending = true))
    }

    @Test fun spokenTextDropsSymbolsTheEngineWouldReadLiterally() {
        assertEquals("Bữa tối, thứ ba. Món canh: Canh bí thành Canh chua cá",
            AssistantVoice.spoken("Bữa tối · thứ ba\nMón canh: Canh bí → Canh chua cá"))
        assertEquals("Đổi tên thành Nhà bà Tư", AssistantVoice.spoken("Đổi tên thành “Nhà bà Tư”"))
    }

    @Test fun longPlansAreSummarizedAndShortOnesReadInFull() {
        val week = AssistantVoice.proposalPrompt(List(20) { "x" }, meals = 14, addressAs = "bác")
        assertTrue(week.startsWith("Mình đề xuất đổi món cho 14 bữa. Bác xem trên màn hình."))
        val one = AssistantVoice.proposalPrompt(listOf("Bữa tối\nMón canh: Canh bí → Canh chua cá"), meals = 1, addressAs = "bạn")
        assertTrue(one.contains("Canh chua cá")); assertTrue(one.endsWith("Bạn nói đồng ý để áp dụng, hoặc không để hủy."))
    }

    @Test fun messagesUseTheChosenFormOfAddress() {
        assertTrue(AssistantVoice.error(VoiceErrorKind.NotHeard, "bà").contains("Bà bấm nút"))
        assertTrue(AssistantVoice.askAgain("  ").contains("Bạn nói"))
        assertEquals("bạn", VoiceSettings.clean(""))
        assertEquals(20, VoiceSettings.clean("x".repeat(40)).length)
    }

    @Test fun addressFormReachesTheModelContext() {
        val state = AssistantTestCatalog.state().copy(voice = VoiceSettings(addressAs = "bác"))
        val context = AssistantGateway.snapshot(AssistantTestCatalog.engine, state, AssistantTestCatalog.day, Screen.TODAY, null)
        assertEquals("bác", context.getString("address_as"))
    }
}
