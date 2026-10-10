package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.AccentLocale
import com.example.data.remote.Content
import com.example.data.remote.GeminiRetrofitClient
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.PrebuiltVoiceConfig
import com.example.data.remote.SpeechConfig
import com.example.data.remote.VoiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class EnglishSpeechManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isNativeTtsReady = false
    private var currentAudioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeUtteranceTag = MutableStateFlow<String?>(null)
    val activeUtteranceTag: StateFlow<String?> = _activeUtteranceTag.asStateFlow()

    private val _selectedAccent = MutableStateFlow(AccentLocale.US)
    val selectedAccent: StateFlow<AccentLocale> = _selectedAccent.asStateFlow()

    private val _currentRate = MutableStateFlow(1.0f)
    val currentRate: StateFlow<Float> = _currentRate.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val langResult = engine.setLanguage(Locale.US)
                        isNativeTtsReady = langResult != TextToSpeech.LANG_MISSING_DATA &&
                            langResult != TextToSpeech.LANG_NOT_SUPPORTED
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                                _activeUtteranceTag.value = null
                                _statusBanner.value = null
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                _activeUtteranceTag.value = null
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                                _activeUtteranceTag.value = null
                            }
                        })
                    }
                } else {
                    isNativeTtsReady = false
                }
            }
        } catch (_: Exception) {
            isNativeTtsReady = false
        }
    }

    fun setAccent(accent: AccentLocale) {
        _selectedAccent.value = accent
        val locale = if (accent == AccentLocale.UK) Locale.UK else Locale.US
        try {
            tts?.language = locale
        } catch (_: Exception) {
            // Ignore if engine not ready yet
        }
    }

    fun speakEnglish(
        text: String,
        speedRate: Float = 1.0f,
        tag: String = "full_paragraph"
    ) {
        if (text.isBlank()) return
        stop()

        _currentRate.value = speedRate
        _activeUtteranceTag.value = tag
        val speedLabel = if (speedRate < 0.85f) "Lambat (0.7x)" else "Normal (1.0x)"
        _statusBanner.value = "Memutar pengucapan ${_selectedAccent.value.shortCode} • $speedLabel"

        val engine = tts
        if (isNativeTtsReady && engine != null) {
            try {
                val locale = if (_selectedAccent.value == AccentLocale.UK) Locale.UK else Locale.US
                engine.language = locale
                engine.setSpeechRate(speedRate)
                engine.setPitch(1.0f)
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val utteranceId = UUID.randomUUID().toString()
                val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result == TextToSpeech.SUCCESS) {
                    _isSpeaking.value = true
                    return
                }
            } catch (_: Exception) {
                // Fall back to Gemini TTS or visual reading timer
            }
        }

        // Fallback: Try Gemini TTS API if API key is configured, else paced reading highlight
        playbackJob = scope.launch {
            _isSpeaking.value = true
            val playedViaCloud = tryPlayGeminiCloudTts(text, speedRate)
            if (!playedViaCloud) {
                // Paced guided reading fallback when running in an emulator without preinstalled TTS voice data
                val wordCount = text.split(Regex("\\s+")).size.coerceAtLeast(2)
                val durationMs = ((wordCount * 420L) / speedRate).toLong().coerceIn(1500L, 12000L)
                delay(durationMs)
            }
            _isSpeaking.value = false
            _activeUtteranceTag.value = null
            _statusBanner.value = null
        }
    }

    private suspend fun tryPlayGeminiCloudTts(text: String, speedRate: Float): Boolean =
        withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY.trim()
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("YOUR_")) {
                return@withContext false
            }
            try {
                val styleInstruction = if (speedRate < 0.85f) {
                    "Speak slowly, clearly, and distinctly for an English learner: $text"
                } else {
                    "Speak clearly in standard ${_selectedAccent.value.labelId} English: $text"
                }
                val voiceName = if (_selectedAccent.value == AccentLocale.UK) "Puck" else "Kore"
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = styleInstruction)))),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            voiceConfig = VoiceConfig(
                                prebuiltVoiceConfig = PrebuiltVoiceConfig(voiceName = voiceName)
                            )
                        )
                    )
                )
                val response = GeminiRetrofitClient.service.generateSpeech(apiKey, request)
                val inlineData = response.candidates.firstOrNull()
                    ?.content?.parts?.firstOrNull { it.inlineData != null }
                    ?.inlineData ?: return@withContext false

                val pcmBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                if (pcmBytes.isEmpty()) return@withContext false

                val sampleRate = 24000
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(pcmBytes.size)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                currentAudioTrack = audioTrack
                audioTrack.write(pcmBytes, 0, pcmBytes.size)
                audioTrack.play()

                val durationMs = (pcmBytes.size.toLong() * 1000L) / (sampleRate * 2)
                delay(durationMs + 250L)
                audioTrack.stop()
                audioTrack.release()
                currentAudioTrack = null
                true
            } catch (_: Exception) {
                false
            }
        }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
            currentAudioTrack = null
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
        _activeUtteranceTag.value = null
        _statusBanner.value = null
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
