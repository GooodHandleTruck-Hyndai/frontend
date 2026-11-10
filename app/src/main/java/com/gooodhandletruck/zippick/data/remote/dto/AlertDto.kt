package com.gooodhandletruck.zippick.data.remote.dto

import com.gooodhandletruck.zippick.data.model.LocalDateSerializer
import com.gooodhandletruck.zippick.data.model.OffsetDateTimeSerializer
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/*
 * 관심 건물 알림 API의 응답 data. 이름과 모양은 백엔드 domain/alert/dto와 같다. 모두 X-Anonymous-Id 헤더가 필요하다.
 * 서버 배치가 하루 한 번(04:30) 새 거래를 찾아 알림을 만들고, 앱은 목록 API로 가져간다.
 * 관심 조건에 맞는 거래와 저장한 건물의 거래가 대상이고, 관심 조건이 없거나 알림을 껐으면 만들지 않는다.
 */

/** 관심 건물 알림 목록(GET /api/v1/alerts). 최신순, 최대 100개 */
@Serializable
data class AlertListResponse(val alerts: List<AlertResponse> = emptyList())

/**
 * 알림 한 건.
 *
 * @property buildingName 단지 이름
 * @property deal 알림을 만든 새 거래
 * @property read 읽었는지. 읽은 알림은 배지 개수에서 빠진다
 * @property createdAt 알림을 만든 시각(+09:00)
 */
@Serializable
data class AlertResponse(
    val alertId: Long,
    val buildingId: Long,
    val buildingName: String,
    val deal: AlertDeal,
    val read: Boolean = false,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null
)

/**
 * 알림을 만든 거래. 모양은 주변 건물의 [LatestDeal]과 같다.
 *
 * @property price 거래금액(만원)
 * @property areaM2 전용면적 ㎡(소수 2자리)
 * @property areaPyeong 전용면적 평(소수 1자리)
 */
@Serializable
data class AlertDeal(
    val price: Int,
    @Serializable(with = LocalDateSerializer::class)
    val dealDate: LocalDate,
    val areaM2: Double,
    val areaPyeong: Double
)

/** 알림 읽음(PATCH /api/v1/alerts/{alertId}/read). [read]는 늘 true, [readAt]은 처음 읽은 시각이다 */
@Serializable
data class AlertReadResponse(
    val alertId: Long,
    val read: Boolean,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val readAt: OffsetDateTime? = null
)
