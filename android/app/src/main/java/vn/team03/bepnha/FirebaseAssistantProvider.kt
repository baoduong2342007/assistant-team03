package vn.team03.bepnha

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

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
            requestOptions = RequestOptions(timeoutInMillis = BuildConfig.ASSISTANT_TIMEOUT_MS),
        )
    }

    /** One chat per user turn, so thought signatures and tool calls stay paired with their results. */
    override fun open(payload: JSONObject): AssistantSession {
        var chat: Chat? = null
        return AssistantSession { results ->
            call {
                val current = chat
                if (current == null) {
                    FirebaseCalls.checkAppToken()
                    model.startChat().also { chat = it }.sendMessage(payload.toString())
                } else {
                    require(results.isNotEmpty())
                    // The Gemini Developer API accepts only "user" and "model" roles; tool results go back as "user".
                    current.sendMessage(Content("user", results.map { result ->
                        FunctionResponsePart(result.call.name, Json.parseToJsonElement(result.result.toString()).jsonObject, result.call.id)
                    }))
                }
            }
        }
    }

    /** Single-shot helper kept for live smoke tests. */
    suspend fun generate(payload: JSONObject): AssistantModelReply = open(payload).send(emptyList())

    private suspend fun call(block: suspend () -> GenerateContentResponse): AssistantModelReply {
        if (!BuildConfig.DEBUG || !BuildConfig.FIREBASE_CONFIGURED) throw AssistantFailure(AssistantFailureKind.UNCONFIGURED)
        try {
            val started = System.currentTimeMillis()
            return withTimeout(BuildConfig.ASSISTANT_TIMEOUT_MS) {
                val result = block()
                if (BuildConfig.DEBUG) android.util.Log.d("BepNhaAI", "AI response received in ${System.currentTimeMillis() - started} ms")
                require(result.candidates.size == 1)
                require(result.candidates.single().finishReason == FinishReason.STOP)
                val calls = result.functionCalls
                require(calls.size <= AssistantService.MAX_CALLS_PER_REPLY)
                AssistantModelReply(result.text, calls.map { call ->
                    val args = call.args.entries.joinToString(",", "{", "}") { (key, value) -> "${JSONObject.quote(key)}:$value" }
                    require(args.length <= 100_000)
                    AssistantToolCall(call.name, JSONObject(args), call.id)
                })
            }
        } catch (_: TimeoutCancellationException) { throw AssistantFailure(AssistantFailureKind.TIMEOUT)
        } catch (e: CancellationException) { throw e
        } catch (e: AssistantFailure) { throw e
        } catch (e: Exception) {
            // Debug builds only: the raw SDK reason, which the user-facing message hides.
            if (BuildConfig.DEBUG) android.util.Log.w("BepNhaAI", "AI call failed: ${FirebaseCalls.causes(e).joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message?.take(500)}" }}")
            throw AssistantFailure(FirebaseCalls.classify(e))
        }
    }

    companion object {
        internal fun declarations(): List<FunctionDeclaration> = AgentTools.specs.map { spec ->
            FunctionDeclaration(spec.name, spec.description, spec.parameters.mapValues { schema(it.value) }, spec.optional)
        }

        private fun schema(param: ToolParam): Schema = when (param.type) {
            "string" -> Schema.string(param.description)
            "integer" -> Schema.integer(param.description)
            "boolean" -> Schema.boolean(param.description)
            "enum" -> Schema.enumeration(requireNotNull(param.values), param.description)
            "array" -> Schema.array(schema(requireNotNull(param.items)), param.description, maxItems = param.maxItems)
            "object" -> Schema.obj(param.properties.mapValues { schema(it.value) }, param.optional, param.description)
            else -> error("Unknown schema type ${param.type}")
        }
    }
}
