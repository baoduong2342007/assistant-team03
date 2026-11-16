package vn.team03.bepnha

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** SDK transport only; tool execution and state changes belong to the app. */
class FirebaseAssistantProvider(context: Context) : AssistantProvider {
    private val instruction = context.assets.open("assistant-system.txt").bufferedReader().use { it.readText() }
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI(), useLimitedUseAppCheckTokens = true).generativeModel(
            modelName = BuildConfig.ASSISTANT_MODEL,
            systemInstruction = content { text(instruction) },
            generationConfig = generationConfig {
                maxOutputTokens = 4096; candidateCount = 1
                thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
            },
            tools = listOf(Tool.functionDeclarations(declarations())),
            toolConfig = ToolConfig(FunctionCallingConfig.auto()),
            requestOptions = RequestOptions(timeoutInMillis = 25_000),
        )
    }

    override suspend fun generate(payload: JSONObject): AssistantModelReply {
        if (!BuildConfig.DEBUG || !BuildConfig.FIREBASE_CONFIGURED) throw AssistantFailure(AssistantFailureKind.UNCONFIGURED)
        try {
            return withTimeout(25_000) {
                // Fail explicitly on unregistered demo devices instead of silently sending an invalid token.
                checkAppToken()
                if (BuildConfig.DEBUG) android.util.Log.d("BepNhaAI", "App Check verified; starting AI request")
                val result = model.generateContent(payload.toString())
                if (BuildConfig.DEBUG) android.util.Log.d("BepNhaAI", "AI response received")
                require(result.candidates.size == 1)
                require(result.candidates.single().finishReason == FinishReason.STOP)
                val calls = result.functionCalls
                require(calls.size <= 1)
                AssistantModelReply(result.text, calls.map { call ->
                    val args = call.args.entries.joinToString(",", "{", "}") { (key, value) -> "${JSONObject.quote(key)}:$value" }
                    require(args.length <= 100_000)
                    AssistantToolCall(call.name, JSONObject(args))
                })
            }
        } catch (_: TimeoutCancellationException) { throw AssistantFailure(AssistantFailureKind.TIMEOUT)
        } catch (e: CancellationException) { throw e
        } catch (e: AssistantFailure) { throw e
        } catch (e: Exception) { throw AssistantFailure(classify(e)) }
    }

    private suspend fun checkAppToken() = suspendCancellableCoroutine<Unit> { continuation ->
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
            .addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(AssistantFailure(
                    if (causes(error).any { it is IOException || it is FirebaseNetworkException }) AssistantFailureKind.NETWORK else AssistantFailureKind.CONFIGURATION))
            }
    }

    private fun classify(error: Exception): AssistantFailureKind {
        val chain = causes(error)
        return when {
            error is QuotaExceededException -> AssistantFailureKind.QUOTA
            error is RequestTimeoutException -> AssistantFailureKind.TIMEOUT
            error is InvalidAPIKeyException || error is APINotConfiguredException || error is ServiceDisabledException || error is UnsupportedUserLocationException -> AssistantFailureKind.CONFIGURATION
            error is PromptBlockedException || error is ContentBlockedException -> AssistantFailureKind.BLOCKED
            error is SerializationException || error is ResponseStoppedException || error is IllegalArgumentException || error is org.json.JSONException -> AssistantFailureKind.INVALID
            chain.any { it is IOException || it is FirebaseNetworkException } -> AssistantFailureKind.NETWORK
            error is ServerException -> classifyServerMessage(error.message.orEmpty())
            else -> AssistantFailureKind.CONFIGURATION
        }
    }

    companion object {
        private fun causes(error: Throwable): List<Throwable> = generateSequence(error) { it.cause?.takeUnless { next -> next === it } }.take(8).toList()

        // The SDK exposes a ServerException message, not an HTTP status. Never display that raw body.
        internal fun classifyServerMessage(message: String): AssistantFailureKind = when {
            Regex("(?i)429|RESOURCE_EXHAUSTED|quota").containsMatchIn(message) -> AssistantFailureKind.QUOTA
            Regex("(?i)504|DEADLINE_EXCEEDED|timed? ?out").containsMatchIn(message) -> AssistantFailureKind.TIMEOUT
            Regex("(?i)500|502|503|UNAVAILABLE|overload|internal error").containsMatchIn(message) -> AssistantFailureKind.UNAVAILABLE
            else -> AssistantFailureKind.CONFIGURATION
        }

        internal fun declarations(): List<FunctionDeclaration> {
            val fields = linkedMapOf(
                "type" to Schema.enumeration(AssistantActionType.entries.map { it.name }),
                "date" to Schema.string("Ngày YYYY-MM-DD trong thực đơn bảy ngày"),
                "meal" to Schema.enumeration(listOf("lunch", "dinner")),
                "screen" to Schema.enumeration(Screen.entries.map { it.name }),
                "recipe_id" to Schema.string("ID công thức trong catalog"),
                "index" to Schema.integer("0=mặn, 1=canh, 2=rau"),
                "people" to Schema.integer("Số người từ 1 đến 12"),
                "time" to Schema.string("Giờ ăn HH:mm"), "enabled" to Schema.boolean(),
                "minutes" to Schema.integer("Số phút từ 1 đến 180"), "label" to Schema.string(), "key" to Schema.string("Key chính xác từ context"),
            )
            return listOf(
                FunctionDeclaration("propose_actions", "Đề xuất thao tác để ứng dụng kiểm tra và người dùng xác nhận.", mapOf(
                    "reply" to Schema.string(), "actions" to Schema.array(Schema.obj(fields, fields.keys.drop(3)), maxItems = 8))),
                FunctionDeclaration("suggest_meals", "Tìm mâm theo nguyên liệu người dùng đang có; ứng dụng tự chọn món.", mapOf(
                    "date" to Schema.string("YYYY-MM-DD"), "meal" to Schema.enumeration(listOf("lunch", "dinner")),
                    "pantry_items" to Schema.array(Schema.string(), minItems = 1, maxItems = 30),
                    "exclude_ingredients" to Schema.array(Schema.string(), maxItems = 30), "pantry_only" to Schema.boolean()),
                    optionalParameters = listOf("exclude_ingredients", "pantry_only")),
            )
        }
    }
}
