package vn.team03.bepnha

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime

enum class AssistantSource { AI, LOCAL, SYSTEM }

/** UI metadata is deliberately excluded from the HTTP payload. */
data class AssistantChatMessage(
    val role: String,
    val text: String,
    val recipeIds: List<String> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: AssistantSource = AssistantSource.AI,
)
data class AssistantMealSuggestion(
    val id: Int,
    val date: LocalDate,
    val meal: Meal,
    val recipeIds: List<String>,
    val pantryKeys: Set<String>,
)

data class AssistantReply(
    val text: String,
    val actions: List<AssistantAction>,
    val suggestions: List<AssistantMealSuggestion> = emptyList(),
)

data class AssistantProposal(val base: PlannerState, val actions: List<AssistantAction>, val descriptions: List<String>) {
    val canReviseMenu: Boolean
        get() {
            val first = actions.firstOrNull() ?: return false
            return actions.all { it.type == AssistantActionType.swap_recipe && it.targetsSameMealAs(first) }
        }
}

object AssistantGateway {
    fun snapshot(
        engine: PlannerEngine,
        state: PlannerState,
        today: LocalDate,
        screen: Screen,
        recipeId: String?,
        pendingMenuChanges: List<AssistantAction> = emptyList(),
        lastMenuChanges: List<AssistantAction> = emptyList(),
    ): JSONObject {
        val meals = JSONArray()
        state.plans.keys.sorted().forEach { key ->
            val parts = key.split('|'); val date = LocalDate.parse(parts[0]); val meal = Meal.from(parts[1]) ?: return@forEach
            if (date !in today..today.plusDays(6)) return@forEach
            val menu = engine.menu(state, date, meal)
            meals.put(JSONObject().put("date", date).put("meal", meal.value).put("recipes", JSONArray(menu.map { it.id }))
                .put("cost", engine.cost(menu, state.profile.people)).put("minutes", engine.minutes(menu))
                .put("reminder", key in state.reminders)
                .put("ingredients", JSONArray(engine.ingredients(menu, state.profile.people).map { i ->
                    JSONObject().put("key", i.key).put("amount", i.amount).put("checked", i.key in state.checked[key].orEmpty()) }))
                .put("tasks", JSONArray(engine.timeline(state, date, meal).map { t ->
                    JSONObject().put("key", t.key).put("title", t.title).put("time", PlannerEngine.time(t.at)).put("done", t.key in state.done[key].orEmpty()) })))
        }
        return JSONObject().put("today", today).put("timezone", PlannerEngine.zone.id)
            .put("date", state.date).put("meal", state.meal.value).put("screen", screen.name).put("recipe_id", recipeId ?: JSONObject.NULL)
            .put("people", state.profile.people).put("lunch", state.profile.lunch).put("dinner", state.profile.dinner)
            .put("meals", meals).put("favorites", JSONArray(state.favorites.toList()))
            .put("timer", state.timer?.let { JSONObject().put("label", it.label).put("end", it.end).put("date", it.date).put("meal", it.meal.value) } ?: JSONObject.NULL)
            .apply {
                if (pendingMenuChanges.isNotEmpty()) put("pending_menu_changes", menuChangesJson(pendingMenuChanges))
                if (lastMenuChanges.isNotEmpty()) put("last_menu_changes", menuChangesJson(lastMenuChanges))
            }
    }

    private fun menuChangesJson(changes: List<AssistantAction>) = JSONArray(changes.map { action ->
        JSONObject().put("date", action.date).put("meal", action.meal.value)
            .put("index", action.index).put("recipe_id", action.recipeId)
    })

    fun ask(question: String, history: List<AssistantChatMessage>, context: JSONObject): AssistantReply {
        val base = BuildConfig.ASSISTANT_BASE_URL.trimEnd('/')
        require(base.isNotBlank()) { "Chưa cấu hình địa chỉ trợ lý. Bạn vẫn có thể tìm trong công thức có sẵn." }
        val url = URL("$base/api/assistant/turn")
        require(url.protocol == "https" && url.userInfo == null) { "Địa chỉ trợ lý phải dùng HTTPS." }
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000; connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = JSONObject().put("message", question).put("context", context)
                .put("history", JSONArray(history.takeLast(20).map { JSONObject().put("role", it.role).put("text", it.text) }))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            check(status == 200) { failureMessage(status, connection.getHeaderField("Retry-After")?.toLongOrNull()) }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1_000_000)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.size <= 1_000_000)
            return parseReply(JSONObject(String(bytes, Charsets.UTF_8)))
        } catch (exception: java.io.IOException) {
            throw IllegalStateException("Chưa kết nối được trợ lý. Hãy kiểm tra mạng, thử lại hoặc tìm trong công thức có sẵn.", exception)
        } catch (exception: org.json.JSONException) {
            throw IllegalArgumentException("Phản hồi trợ lý chưa hợp lệ. Hãy thử lại hoặc tìm trong công thức có sẵn.", exception)
        } catch (exception: IllegalArgumentException) {
            throw IllegalStateException("Phản hồi trợ lý chưa hợp lệ. Hãy thử lại hoặc tìm trong công thức có sẵn.", exception)
        } finally { connection.disconnect() }
    }

    internal fun failureMessage(status: Int, retryAfterSeconds: Long? = null): String = when (status) {
        429 -> {
            val seconds = retryAfterSeconds?.takeIf { it in 1L..604800L }
            val wait = when {
                seconds == null -> "Hãy kiểm tra hạn mức hoặc thử lại sau."
                seconds >= 3600L -> "Dịch vụ yêu cầu chờ khoảng ${(seconds + 3599) / 3600} giờ trước khi thử lại."
                else -> "Dịch vụ yêu cầu chờ khoảng ${(seconds + 59) / 60} phút trước khi thử lại."
            }
            "AI đang đạt giới hạn yêu cầu hoặc hạn mức. $wait Bạn vẫn có thể tìm trong công thức có sẵn."
        }
        504 -> "AI phản hồi quá chậm. Hãy thử lại hoặc tìm trong công thức có sẵn."
        502 -> "Phản hồi chưa hợp lệ. Hãy thử diễn đạt rõ ngày, bữa và món."
        413 -> "Ngữ cảnh bữa ăn quá lớn. Hãy đóng trợ lý, mở lại rồi gửi câu hỏi."
        422 -> "Câu hỏi hoặc ngữ cảnh chưa hợp lệ. Hãy chọn lại bữa rồi gửi câu hỏi."
        else -> "AI chưa sẵn sàng. Hãy thử lại hoặc tìm trong công thức có sẵn."
    }

    internal fun parseReply(result: JSONObject): AssistantReply {
        val actions = result.getJSONArray("actions")
        val suggestions = if (result.has("suggestions")) result.getJSONArray("suggestions") else JSONArray()
        require(actions.length() <= 8 && suggestions.length() <= 2)
        require(actions.length() == 0 || suggestions.length() == 0)
        return AssistantReply(
            result.getString("reply"),
            (0 until actions.length()).map { parseAction(actions.getJSONObject(it)) },
            (0 until suggestions.length()).map { index ->
                val suggestion = suggestions.getJSONObject(index)
                val id = suggestion.get("id") as? Int
                require(id == index + 1)
                val recipes = suggestion.getJSONArray("recipe_ids")
                val pantry = suggestion.getJSONArray("pantry_keys")
                require(recipes.length() == 3 && pantry.length() in 1..60)
                AssistantMealSuggestion(
                    id = requireNotNull(id),
                    date = LocalDate.parse(suggestion.getString("date")),
                    meal = requireNotNull(Meal.from(suggestion.getString("meal"))),
                    recipeIds = (0 until recipes.length()).map { recipes.getString(it) },
                    pantryKeys = (0 until pantry.length()).map { pantry.getString(it) }.toSet(),
                )
            },
        )
    }

    fun parseAction(o: JSONObject): AssistantAction {
        fun text(key: String) = if (!o.has(key) || o.isNull(key)) null else (o.get(key) as? String ?: error("Tham số chữ không hợp lệ"))
        fun number(key: String): Int? {
            if (!o.has(key) || o.isNull(key)) return null
            return (o.get(key) as? Int) ?: error("Tham số số không hợp lệ")
        }
        val type = AssistantActionType.valueOf(o.getString("type"))
        val fields = when (type) {
            AssistantActionType.navigate -> setOf("screen")
            AssistantActionType.open_recipe -> setOf("recipe_id")
            AssistantActionType.swap_recipe -> setOf("recipe_id", "index")
            AssistantActionType.set_people -> setOf("people")
            AssistantActionType.set_meal_time -> setOf("time")
            AssistantActionType.set_reminder -> setOf("enabled")
            AssistantActionType.start_timer -> setOf("minutes", "label")
            AssistantActionType.cancel_timer -> emptySet()
            AssistantActionType.set_ingredient, AssistantActionType.set_task -> setOf("key", "enabled")
            AssistantActionType.set_favorite -> setOf("recipe_id", "enabled")
        }
        require(o.keys().asSequence().all { it in fields + setOf("type", "date", "meal") || o.isNull(it) })
        require(fields.all { o.has(it) && !o.isNull(it) })
        return AssistantAction(type, LocalDate.parse(o.getString("date")), requireNotNull(Meal.from(o.getString("meal"))),
            text("screen")?.let(Screen::valueOf), text("recipe_id"), number("index"), number("people"), text("time")?.let(LocalTime::parse),
            if (o.has("enabled") && !o.isNull("enabled")) o.get("enabled") as? Boolean ?: error("Trạng thái không hợp lệ") else null,
            number("minutes"), text("label"), text("key"))
    }
}
