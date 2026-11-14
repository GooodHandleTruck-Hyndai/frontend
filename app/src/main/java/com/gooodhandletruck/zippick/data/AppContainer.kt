package com.gooodhandletruck.zippick.data

import android.content.Context
import com.gooodhandletruck.zippick.data.remote.ServerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 앱에 하나씩만 두는 객체들. DI 프레임워크 없이 여기서 만들어 ViewModel에 나눠 준다. */
class AppContainer(context: Context) {

    /** 화면과 상관없이 앱이 살아 있는 동안 쓰는 작업 범위. 여러 화면이 위치 구독 하나를 나눠 쓴다 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 백엔드 통신(기기 등록·X-Anonymous-Id 헤더 포함) */
    private val server = ServerClient(context)

    /** 단지 데이터. 서버 없이 화면만 볼 때는 FakeRepository(context)로 바꾼다 */
    val complexRepository: ComplexRepository = RemoteRepository(context, server)

    /** 관심 건물 새 거래 알림(S-10) */
    val alertInboxRepository = AlertInboxRepository(server)

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
        // 알림 조건을 서버 관심 조건으로 보낸다. 위치·알림 설정 저장소가 만들어진 뒤라 맨 아래에 둔다
        InterestConditionSync(
            context = context,
            server = server,
            alertSettings = alertSettingsRepository,
            locationRepository = locationRepository,
            scope = appScope
        ).start()
    }
}
