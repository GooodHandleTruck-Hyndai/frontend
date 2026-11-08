package com.gooodhandletruck.zippick.data.remote.dto

import com.gooodhandletruck.zippick.data.model.OffsetDateTimeSerializer
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/* 저장 API의 요청·응답 data. 이름과 모양은 백엔드 domain/favorite/dto와 같다. 모두 X-Anonymous-Id 헤더가 필요하다. */

/** 저장하기 요청(POST /api/v1/favorites) */
@Serializable
data class FavoriteCreateRequest(val buildingId: Long)

/**
 * 저장한 건물. 저장하기의 data이자 저장 목록의 항목이다. 칸 이름은 건물 상세와 같다.
 * 가격은 오지 않는다.
 *
 * @property jibunAddress 지번 주소(시도는 줄임말)
 * @property lat 서버가 좌표를 아직 못 찾았으면 null
 * @property savedAt 저장한 시각(+09:00)
 */
@Serializable
data class FavoriteResponse(
    val buildingId: Long,
    val name: String,
    val type: String,
    val jibunAddress: String,
    val lat: Double? = null,
    val lon: Double? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val savedAt: OffsetDateTime? = null
)
