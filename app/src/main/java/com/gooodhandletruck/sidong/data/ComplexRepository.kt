package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.NearbyResponse

/**
 * 단지 데이터를 주는 곳. 지금은 [FakeRepository](assets 목업)이고, 서버가 준비되면 RemoteRepository로 바꾼다.
 * 요청·응답 형식은 Claude Docs 'API 명세 초안 검토'의 P0 제안을 따른다.
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

    companion object {
        /** S-03은 단지 카드를 최대 10개 보여 준다 */
        const val NEARBY_LIMIT = 10
    }
}

/** 서버에 없는 단지다(COMPLEX_NOT_FOUND, 404). S-04는 상태 상자를 보여 준다 */
class ComplexNotFoundException(complexId: String) : Exception("없는 단지: $complexId")
