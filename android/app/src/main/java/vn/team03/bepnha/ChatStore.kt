package vn.team03.bepnha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the last [LIMIT] chat messages on the device so the conversation survives closing the app.
 * Only text is stored: proposals, meal cards and errors are never restored, so nothing old can be executed.
 */
class ChatStore(context: Context) {
    private val preferences = context.getSharedPreferences("bep_nha_chat", Context.MODE_PRIVATE)

    fun load(recipes: Set<String>): List<AssistantChatMessage> =
        runCatching { decode(preferences.getString("messages", "[]") ?: "[]", recipes) }.getOrDefault(emptyList())

    fun save(messages: List<AssistantChatMessage>) {
        check(preferences.edit().putString("messages", encode(messages)).commit()) { "Could not save chat" }
    }

    companion object {
        const val LIMIT = 30
        /** Older messages are kept for reading but not sent to the model ("mai" from last week would mislead it). */
        const val MODEL_WINDOW_MS = 6 * 60 * 60_000L

        fun keep(messages: List<AssistantChatMessage>, unanswered: Set<String> = emptySet()) =
            messages.filter { !it.id.startsWith("greeting-") && it.id !in unanswered && it.text.isNotBlank() }.takeLast(LIMIT)

        fun encode(messages: List<AssistantChatMessage>): String = JSONArray(keep(messages).map { message ->
            JSONObject().put("id", message.id).put("role", message.role).put("text", message.text.take(8000))
                .put("recipes", JSONArray(message.recipeIds)).put("source", message.source.name)
                .put("notice", message.notice ?: JSONObject.NULL).put("at", message.at)
        }).toString()

        /** Defensive parse: unknown roles are dropped and recipe links are kept only for recipes that still exist. */
        fun decode(json: String, recipes: Set<String>): List<AssistantChatMessage> {
            val list = JSONArray(json)
            return (0 until list.length()).mapNotNull { i ->
                val item = list.optJSONObject(i) ?: return@mapNotNull null
                val role = item.optString("role").takeIf { it == "user" || it == "assistant" } ?: return@mapNotNull null
                val text = item.optString("text").take(8000).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                AssistantChatMessage(role, text,
                    item.optJSONArray("recipes")?.let { ids -> (0 until ids.length()).map(ids::getString).filter { it in recipes } }.orEmpty(),
                    id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    source = AssistantSource.entries.firstOrNull { it.name == item.optString("source") } ?: AssistantSource.SYSTEM,
                    notice = if (item.isNull("notice")) null else item.optString("notice"),
                    at = item.optLong("at", 0L))
            }.takeLast(LIMIT)
        }
    }
}
