package com.gooodhandletruck.zippick.data

import android.content.Context
import android.util.Log
import com.gooodhandletruck.zippick.data.remote.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 앱에 하나씩만 두는 객체들. DI 프레임워크 없이 여기서 만들어 ViewModel에 나눠 준다. */
class AppContainer(context: Context) {

    /** 화면과 상관없이 앱이 살아 있는 동안 쓰는 작업 범위. 여러 화면이 위치 구독 하나를 나눠 쓴다 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 백엔드 서버 창구(공통 헤더·기기 등록·실패 응답). 저장소를 하나씩 서버로 옮길 때 쓴다 */
    val apiClient = ApiClient.create(context)

    /** 서버가 준비되면 RemoteRepository로 바꾼다. 서버 호출은 [apiClient]로 한다 */
    val complexRepository: ComplexRepository = FakeRepository(context)

    /** 차 위치와 향한 쪽(플레오스 FusedLocation) */
    val locationRepository = LocationRepository(context, appScope)

    /** 길 안내와 도착(플레오스 NaviHelper) */
    val navigationRepository = NavigationRepository(context)

    /** 주행 중 자동 알림 조건과 켜기·끄기(S-02·S-07) */
    val alertSettingsRepository = AlertSettingsRepository(context)

    /** S-03 범위 칩의 기본값(S-07) */
    val nearbyRangeStore = NearbyRangeStore(context)

    /** 오늘 소리로 알려 준 단지(하루에 한 번만 자동 알림) */
    val announcedStore = AnnouncedStore(context)

    /** 주행 중 음성 안내(플레오스 TextToSpeech) */
    val speechRepository = SpeechRepository(context, appScope)

    init {
        registerDeviceInBackground()
    }

    /**
     * 첫 실행이면 서버에 기기를 등록해 익명 ID를 받아 둔다. 이미 받아 두었으면 서버를 부르지 않는다.
     * 화면 없이 뒤에서 하고, 서버가 꺼져 있어도 앱은 그대로 쓴다. 기기 ID가 필요한 API를 처음 부를 때 다시 등록한다.
     */
    private fun registerDeviceInBackground() {
        appScope.launch {
            try {
                Log.i(TAG, "서버 기기 ID: ${apiClient.ensureDevice()}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "기기 등록을 못 했어요. 기기 ID가 필요한 API를 부를 때 다시 해요", e)
            }
        }
    }

    private companion object {
        const val TAG = "ApiClient"
    }
}
