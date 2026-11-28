package vn.team03.bepnha

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.ai.type.*
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** App Check and error mapping shared by the chat assistant and the "Nấu cùng tôi" Live session. */
internal object FirebaseCalls {
    // Fail explicitly on unregistered demo devices instead of silently sending an invalid token.
    suspend fun checkAppToken() = suspendCancellableCoroutine<Unit> { continuation ->
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
            .addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
            .addOnFailureListener { error ->
                // Debug builds only: the reason (e.g. "403 App attestation failed") helps register the device.
                if (BuildConfig.DEBUG) android.util.Log.w("BepNhaAI", "App Check token failed: ${causes(error).joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message}" }}")
                if (continuation.isActive) continuation.resumeWithException(AssistantFailure(
                    if (causes(error).any { it is IOException || it is FirebaseNetworkException }) AssistantFailureKind.NETWORK else AssistantFailureKind.CONFIGURATION))
            }
    }

    fun classify(error: Exception): AssistantFailureKind {
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

    fun causes(error: Throwable): List<Throwable> = generateSequence(error) { it.cause?.takeUnless { next -> next === it } }.take(8).toList()

    // The SDK exposes a ServerException message, not an HTTP status. Never display that raw body.
    fun classifyServerMessage(message: String): AssistantFailureKind = when {
        Regex("(?i)429|RESOURCE_EXHAUSTED|quota").containsMatchIn(message) -> AssistantFailureKind.QUOTA
        Regex("(?i)504|DEADLINE_EXCEEDED|timed? ?out").containsMatchIn(message) -> AssistantFailureKind.TIMEOUT
        Regex("(?i)500|502|503|UNAVAILABLE|overload|internal error").containsMatchIn(message) -> AssistantFailureKind.UNAVAILABLE
        else -> AssistantFailureKind.CONFIGURATION
    }
}
