package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test

class ChatStoreTest {
    private val recipes = setOf("tofu", "sour")

    @Test fun textRoundTripsWithoutGreetingsOrUnansweredQuestions() {
        val messages = listOf(
            AssistantChatMessage("assistant", "Chào bác", id = "greeting-1", source = AssistantSource.SYSTEM),
            AssistantChatMessage("user", "Tối mai nấu canh chua cá", id = "q1", at = 10),
            AssistantChatMessage("assistant", "Mình đề xuất…", listOf("sour", "gone"), id = "a1", notice = "Đang dùng trợ lý trên máy.", source = AssistantSource.LOCAL, at = 11),
            AssistantChatMessage("user", "Câu chưa được trả lời", id = "q2"),
        )
        val kept = ChatStore.keep(messages, unanswered = setOf("q2"))
        assertEquals(listOf("q1", "a1"), kept.map { it.id })
        val restored = ChatStore.decode(ChatStore.encode(kept), recipes)
        assertEquals(listOf("sour"), restored[1].recipeIds)
        assertEquals(AssistantSource.LOCAL, restored[1].source); assertEquals("Đang dùng trợ lý trên máy.", restored[1].notice)
        assertEquals(10L, restored[0].at)
    }

    @Test fun onlyTheLatestMessagesAreKept() {
        val many = (1..50).map { AssistantChatMessage("user", "Câu $it", id = "m$it") }
        val restored = ChatStore.decode(ChatStore.encode(many), recipes)
        assertEquals(ChatStore.LIMIT, restored.size); assertEquals("m50", restored.last().id)
    }

    @Test fun damagedDataIsSkippedSafely() {
        val json = """[{"role":"system","text":"x"},{"role":"user","text":""},{"role":"user","text":"ok","source":"???"},"bad"]"""
        val restored = ChatStore.decode(json, recipes)
        assertEquals(1, restored.size); assertEquals("ok", restored.single().text)
        assertEquals(AssistantSource.SYSTEM, restored.single().source)
    }
}
