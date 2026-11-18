package vn.team03.bepnha.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceControllerTest {
    private class FakeRecognizer(var available: Boolean = true, var permitted: Boolean = true) : RecognizerEngine {
        var listener: RecognizerListener? = null
        val starts = mutableListOf<Boolean>()
        var stops = 0
        var destroyed = false
        override fun isAvailable() = available
        override fun hasPermission() = permitted
        override fun start(preferOffline: Boolean, listener: RecognizerListener) { starts += preferOffline; this.listener = listener }
        override fun stop() { stops++ }
        override fun cancel() = Unit
        override fun destroy() { destroyed = true }
    }

    private class FakeSpeaker(override val maxInputLength: Int = 4000) : SpeakerEngine {
        lateinit var listener: SpeakerListener
        val spoken = mutableListOf<Triple<String, Float, String>>()
        var stops = 0
        var shutdown = false
        override fun init(listener: SpeakerListener) { this.listener = listener }
        override fun speak(text: String, rate: Float, utteranceId: String, flush: Boolean) { spoken += Triple(text, rate, utteranceId) }
        override fun stop() { stops++ }
        override fun shutdown() { shutdown = true }
    }

    private val recognizer = FakeRecognizer()
    private val speaker = FakeSpeaker()
    private val voice = VoiceController(recognizer, speaker)

    private fun ready() = speaker.listener.onInit(success = true, vietnamese = true)

    @Test fun listeningReportsPartialThenResult() {
        voice.start()
        assertEquals(VoiceState.Listening(""), voice.state.value)
        assertEquals(listOf(true), recognizer.starts) // offline preferred
        recognizer.listener!!.onPartial("đổi món ")
        assertEquals(VoiceState.Listening("đổi món"), voice.state.value)
        recognizer.listener!!.onFinal(" đổi món canh ")
        assertEquals(VoiceState.Result("đổi món canh"), voice.state.value)
    }

    @Test fun emptyResultMeansNotHeard() {
        voice.start()
        recognizer.listener!!.onFinal(null)
        assertEquals(VoiceState.Error(VoiceErrorKind.NotHeard), voice.state.value)
    }

    @Test fun missingRecognizerOrPermissionFailsWithoutListening() {
        recognizer.permitted = false
        voice.start()
        assertEquals(VoiceState.Error(VoiceErrorKind.NoPermission), voice.state.value)
        recognizer.available = false
        voice.start()
        assertEquals(VoiceState.Error(VoiceErrorKind.Unsupported), voice.state.value)
        assertTrue(recognizer.starts.isEmpty())
    }

    @Test fun errorCodesAreClassified() {
        assertEquals(VoiceErrorKind.NotHeard, VoiceController.classify(7)) // NO_MATCH
        assertEquals(VoiceErrorKind.NotHeard, VoiceController.classify(6)) // SPEECH_TIMEOUT
        assertEquals(VoiceErrorKind.NoNetwork, VoiceController.classify(2)) // NETWORK
        assertEquals(VoiceErrorKind.NoNetwork, VoiceController.classify(1)) // NETWORK_TIMEOUT
        assertEquals(VoiceErrorKind.NoPermission, VoiceController.classify(9))
        assertEquals(VoiceErrorKind.Unsupported, VoiceController.classify(12))
    }

    @Test fun missingOfflinePackRetriesOnlineOnce() {
        voice.start()
        recognizer.listener!!.onError(13) // LANGUAGE_UNAVAILABLE
        assertEquals(listOf(true, false), recognizer.starts)
        assertTrue(voice.state.value is VoiceState.Listening)
        recognizer.listener!!.onError(13)
        assertEquals(VoiceState.Error(VoiceErrorKind.Unsupported), voice.state.value)
        assertEquals(2, recognizer.starts.size)
    }

    @Test fun staleCallbacksFromAnEarlierSessionAreIgnored() {
        voice.start()
        val old = recognizer.listener!!
        voice.start()
        old.onFinal("cũ")
        assertEquals(VoiceState.Listening(""), voice.state.value)
    }

    @Test fun stopOnlyAsksTheRecognizerWhileListening() {
        voice.stop()
        assertEquals(0, recognizer.stops)
        voice.start()
        voice.stop()
        assertEquals(1, recognizer.stops)
    }

    @Test fun speechIsQueuedUntilEngineIsReady() {
        assertEquals(SpeechState.Initializing, voice.speech.value)
        voice.speak("Xin chào")
        assertTrue(speaker.spoken.isEmpty())
        ready()
        assertEquals("Xin chào", speaker.spoken.single().first)
        assertEquals(VoiceController.DEFAULT_RATE, speaker.spoken.single().second)
        assertEquals(SpeechState.Speaking, voice.speech.value)
        speaker.listener.onDone(speaker.spoken.single().third)
        assertEquals(SpeechState.Ready, voice.speech.value)
    }

    @Test fun noVietnameseVoiceIsUnsupportedAndSilent() {
        speaker.listener.onInit(success = true, vietnamese = false)
        assertEquals(SpeechState.Unsupported, voice.speech.value)
        voice.speak("Xin chào")
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test fun newSpeechInterruptsAndOldCompletionIsIgnored() {
        ready()
        voice.speak("Một")
        val first = speaker.spoken.last().third
        voice.speak("Hai")
        speaker.listener.onDone(first)
        assertEquals(SpeechState.Speaking, voice.speech.value)
        voice.stopSpeaking()
        assertEquals(SpeechState.Ready, voice.speech.value)
        assertEquals(1, speaker.stops)
    }

    @Test fun listeningStopsSpeech() {
        ready()
        voice.speak("Đang đọc")
        voice.start()
        assertEquals(SpeechState.Ready, voice.speech.value)
        assertEquals(1, speaker.stops)
    }

    @Test fun rateIsClampedAndUsedForNextSpeech() {
        ready()
        voice.setRate(3f)
        voice.speak("Nhanh")
        assertEquals(VoiceController.MAX_RATE, speaker.spoken.last().second)
        voice.setRate(0.7f)
        voice.speak("Chậm")
        assertEquals(0.7f, speaker.spoken.last().second)
    }

    @Test fun longTextIsSplitAndOnlyTheLastPieceEndsSpeaking() {
        val small = FakeSpeaker(maxInputLength = 20)
        val controller = VoiceController(FakeRecognizer(), small)
        small.listener.onInit(success = true, vietnamese = true)
        controller.speak("Bước một rửa rau. Bước hai luộc nước. Bước ba cho rau vào.")
        assertTrue(small.spoken.size > 1)
        assertTrue(small.spoken.all { it.first.length <= 20 })
        small.listener.onDone(small.spoken.first().third)
        assertEquals(SpeechState.Speaking, controller.speech.value)
        small.listener.onDone(small.spoken.last().third)
        assertEquals(SpeechState.Ready, controller.speech.value)
    }

    @Test fun chunkKeepsAllWords() {
        val text = "Một hai ba bốn năm sáu bảy tám chín mười, mười một. Mười hai!"
        val pieces = VoiceController.chunk(text, 12)
        assertEquals(text.split(' '), pieces.joinToString(" ").split(' '))
        assertTrue(pieces.all { it.length <= 12 })
        assertTrue(VoiceController.chunk("   ", 10).isEmpty())
    }

    @Test fun releaseShutsEverythingDown() {
        ready()
        voice.release()
        assertTrue(recognizer.destroyed)
        assertTrue(speaker.shutdown)
        voice.start()
        voice.speak("sau khi giải phóng")
        assertTrue(recognizer.starts.isEmpty())
        assertTrue(speaker.spoken.isEmpty())
        assertFalse(voice.speech.value == SpeechState.Speaking)
    }
}
