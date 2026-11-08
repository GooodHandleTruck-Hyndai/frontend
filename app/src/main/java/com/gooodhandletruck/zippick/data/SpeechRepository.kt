package com.gooodhandletruck.zippick.data

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 주행 중 음성 안내(플레오스 TextToSpeech, ON_DEVICE 모드라 앱 등록이 필요 없다).
 * 말하기 전에 오디오 포커스를 AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK으로 받고, 다 말하면 바로 돌려준다(CLAUDE.md 주행 중 규칙).
 * TextToSpeech SDK는 오디오 포커스를 직접 다루지 않는다(3.3.1 확인).
 * 말하는 중에 들어온 요청은 쌓지 않고 버린다(화면 설계서 '알림 규칙').
 */
class SpeechRepository(context: Context, private val scope: CoroutineScope) {

    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)

    // TODO: 음성의 오디오 용도(usage)가 Pleos 문서에 없다. 내비 안내처럼 음악을 잠깐 줄이고 말하도록
    //  NAVIGATION_GUIDANCE로 둔다. 실제 차에서 내비 안내와 겹칠 때 어떻게 들리는지 확인해야 한다
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()

    private val _speaking = MutableStateFlow(false)

    /** 말하는 중인지 */
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    // 끝났다는 이벤트가 오지 않아도 오디오 포커스를 붙잡고 있지 않게 한다
    private var watchdog: Job? = null

    private val listener = object : EventListener {
        override fun onReady() = Unit

        override fun onStart() = Unit

        override fun onUpdatedRms(rms: Double) = Unit

        override fun onDone() = finish()

        override fun onStop() = finish()

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "음성 안내 오류: $errorType")
            finish()
        }
    }

    init {
        // 처음 말할 때 기다리지 않도록 앱이 시작할 때 준비한다
        tts.initialize(object : OnInitializeListener {
            override fun onSuccess() = Unit

            override fun onFailure(errorType: ErrorType) {
                Log.w(TAG, "음성 안내를 준비하지 못했어요: $errorType")
            }
        })
        tts.addEventListener(listener)
        tts.setAudioAttributes(attributes)
    }

    /** [text]를 한국어로 말한다. 이미 말하는 중이거나 오디오 포커스를 받지 못하면 말하지 않고 false다 */
    fun speak(text: String): Boolean {
        if (!_speaking.compareAndSet(expect = false, update = true)) return false
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "오디오 포커스를 받지 못해 말하지 않아요")
            _speaking.value = false
            return false
        }
        tts.setLocale(Locale.KOREA)
        tts.speak(text)
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(MAX_SPEECH_DURATION)
            Log.w(TAG, "끝났다는 이벤트가 오지 않아 오디오 포커스를 돌려줘요")
            finish()
        }
        return true
    }

    private fun finish() {
        watchdog?.cancel()
        audioManager.abandonAudioFocusRequest(focusRequest)
        _speaking.value = false
    }

    private companion object {
        const val TAG = "Speech"
        val MAX_SPEECH_DURATION = 20.seconds
    }
}
