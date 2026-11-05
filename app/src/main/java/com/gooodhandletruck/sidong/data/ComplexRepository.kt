package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import com.gooodhandletruck.sidong.data.model.HeardResponse
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.data.model.SavedResponse

/**
 * 단지 데이터를 주는 곳. 지금은 [FakeRepository](assets 목업)이고, 서버가 준비되면 RemoteRepository로 바꾼다.
 * 요청·응답 형식은 Claude Docs 'API 명세 초안 검토'의 P0 제안을 따른다.
 * 기기 등록(POST /api/v1/devices)과 X-Device-Id 헤더는 RemoteRepository 안에서 다룬다. 화면은 몰라도 된다.
 */
interface ComplexRepository {

    /** 주변 단지(S-03). GET /api/v1/complexes/nearby?lat&lon&radius&limit */
    suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int = NEARBY_LIMIT): NearbyResponse

    /** 단지 상세(S-04). GET /api/v1/complexes/{complexId}. 없는 단지면 [ComplexNotFoundException]을 던진다 */
    suspend fun detail(complexId: String): ComplexDetail

    /**
     * 저장하기·저장 취소(S-04). PUT·DELETE /api/v1/saved/{complexId}.
     * 이미 저장됐거나 이미 없어도 성공이라, 통신이 끊겨 다시 보내도 결과가 같다.
     */
    suspend fun setSaved(complexId: String, saved: Boolean)

    /** 저장 목록(S-05). GET /api/v1/saved. 최근에 저장한 것부터 온다 */
    suspend fun saved(): SavedResponse

    /**
     * 앞쪽 단지(S-08 [주변 단지 듣기], 화면 설계서의 brief). 차가 향한 쪽 180도 안에서 가장 가까운 단지 하나, 없으면 null.
     * 주행 중에 불러도 되는 서버 호출이다.
     * TODO: 서버 형식이 아직 없다(API 검토 '고칠 것' 8번). 정해지면 요청·응답을 맞춘다
     */
    suspend fun brief(lat: Double, lon: Double, heading: Double): ComplexSummary?

    /**
     * 주행 중에 단지를 말했다는 기록(설계서의 HEARD). 주행 중에 불러도 되는 서버 호출이다.
     * TODO: 서버 형식이 아직 없다(API 초안의 '주행 중 질의 기록'). 정해지면 요청을 맞춘다
     */
    suspend fun recordHeard(complexId: String)

    /** 오늘 들은 단지(S-06, API 초안의 '주행 중 질의 목록'). 오늘 0시부터, 최근 것부터 온다 */
    suspend fun heardToday(): HeardResponse

    /**
     * 근접 알림을 걸 단지(설계서의 geofences). ([lat], [lon]) 둘레에서 알림 조건에 맞는 단지들. 주행 중에 불러도 된다.
     * TODO: 서버 형식이 아직 없다(API 검토 '고칠 것' 8번). 서버는 저장한 조건을 X-Device-Id로 읽을 테니 [conditions]는 목업만 쓴다
     */
    suspend fun geofences(lat: Double, lon: Double, conditions: AlertConditions): List<ComplexSummary>

    companion object {
        /** S-03은 단지 카드를 최대 10개 보여 준다 */
        const val NEARBY_LIMIT = 10
    }
}

/** 서버에 없는 단지다(COMPLEX_NOT_FOUND, 404). S-04는 상태 상자를 보여 준다 */
class ComplexNotFoundException(complexId: String) : Exception("없는 단지: $complexId")
