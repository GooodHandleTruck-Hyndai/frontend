package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import com.gooodhandletruck.sidong.data.model.HeardResponse
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.data.model.SavedResponse

/**
 * 단지 데이터를 주는 곳. 지금은 [FakeRepository](assets 목업)이고, 서버가 준비되면 RemoteRepository로 바꾼다.
 * 경로는 백엔드 노션 'API 목록'을 따른다(2026-11-07 통일). 노션은 단지를 building이라 부르니 {buildingId}에 complexId를 넣는다.
 * 응답 형식은 노션에 아직 없어서 Claude Docs 'API 명세 초안 검토'의 P0 제안(목업과 같은 모양)을 쓴다. 응답 예시를 받으면 맞춘다.
 * 기기 등록(POST /api/v1/devices)과 X-Device-Id 헤더는 RemoteRepository 안에서 다룬다. 화면은 몰라도 된다.
 * TODO: building 하나가 단지 하나인지(동 하나가 아닌지) 백엔드에 확인한다
 */
interface ComplexRepository {

    /**
     * 주변 단지(S-03). GET /api/v1/buildings/nearby
     * TODO: 쿼리 이름(lat·lon·radius·limit)은 노션에 없다. 백엔드와 맞춘다
     */
    suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int = NEARBY_LIMIT): NearbyResponse

    /** 단지 상세(S-04). GET /api/v1/buildings/{buildingId}. 없는 단지면 [ComplexNotFoundException]을 던진다 */
    suspend fun detail(complexId: String): ComplexDetail

    /**
     * 저장하기(S-04·S-09)는 POST /api/v1/favorites, 저장 취소(S-04)는 DELETE /api/v1/favorites/{buildingId}.
     * 앱은 이미 저장됐거나 이미 없어도 성공으로 본다. 통신이 끊겨 다시 보내도 결과가 같아야 한다.
     * TODO: POST 본문 모양과, 이미 저장한 단지를 다시 보내도 성공으로 답하는지 백엔드에 확인한다
     */
    suspend fun setSaved(complexId: String, saved: Boolean)

    /** 저장 목록(S-05). GET /api/v1/favorites. 최근에 저장한 것부터 온다 */
    suspend fun saved(): SavedResponse

    /**
     * 앞쪽 단지(S-08 [주변 단지 듣기], 화면 설계서의 brief). GET /api/v1/buildings/identify
     * 차가 향한 쪽 180도 안에서 가장 가까운 단지 하나, 없으면 null. 주행 중에 불러도 되는 서버 호출이다.
     * TODO: 노션은 후보를 1~3개 준다. 응답 모양이 정해지면 첫 번째 후보를 돌려준다
     */
    suspend fun brief(lat: Double, lon: Double, heading: Double): ComplexSummary?

    /**
     * 주행 중에 단지를 말했다는 기록(설계서의 HEARD). POST /api/v1/voice-queries. 주행 중에 불러도 되는 서버 호출이다.
     * [주변 단지 듣기]로 말한 단지와 자동 알림으로 말한 단지를 모두 남긴다(노션의 '물어본 건물'보다 넓다).
     * TODO: 본문 모양을 백엔드와 맞춘다
     */
    suspend fun recordHeard(complexId: String)

    /**
     * 오늘 들은 단지(S-06). GET /api/v1/voice-queries. 오늘 0시부터, 최근 것부터 온다
     * TODO: 오늘 것만 주는지(아니면 기간 쿼리를 보내는지) 백엔드와 맞춘다
     */
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
