package com.gooodhandletruck.sidong.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 앱에 하나씩만 두는 객체들. DI 프레임워크 없이 여기서 만들어 ViewModel에 나눠 준다. */
class AppContainer(context: Context) {

    /** 화면과 상관없이 앱이 살아 있는 동안 쓰는 작업 범위. 여러 화면이 위치 구독 하나를 나눠 쓴다 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 서버가 준비되면 RemoteRepository로 바꾼다 */
    val complexRepository: ComplexRepository = FakeRepository(context)

    /** 차 위치와 향한 쪽(플레오스 FusedLocation) */
    val locationRepository = LocationRepository(context, appScope)

    /** 길 안내와 도착(플레오스 NaviHelper) */
    val navigationRepository = NavigationRepository(context)

    /** 주행 중 자동 알림 조건과 켜기·끄기(S-02·S-07) */
    val alertSettingsRepository = AlertSettingsRepository(context)

    /** S-03 범위 칩의 기본값(S-07) */
    val nearbyRangeStore = NearbyRangeStore(context)

    /** 주행 중 음성 안내(플레오스 TextToSpeech) */
    val speechRepository = SpeechRepository(context, appScope)
}
