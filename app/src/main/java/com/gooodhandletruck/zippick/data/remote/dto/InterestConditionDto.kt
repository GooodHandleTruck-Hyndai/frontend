package com.gooodhandletruck.zippick.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * 관심 조건 API의 요청·응답 data. 이름과 모양은 백엔드 domain/interest/dto와 같다. 모두 X-Anonymous-Id 헤더가 필요하다.
 * 기기마다 조건이 하나이고, 서버는 알림을 켠 조건에 맞는 새 거래(와 저장한 건물의 새 거래)로 알림을 만든다.
 * 가격은 만원, 면적은 ㎡ 정수다. 범위의 한쪽이 null이면 그쪽은 제한이 없다.
 * 앱의 S-02 알림 조건(AlertConditions)과 달리 지역(시군구 코드)이 꼭 있어야 하고, 가격·면적은 칸 여러 개가 아니라 범위 하나다.
 */

/**
 * 관심 조건 저장 요청(PUT /api/v1/interest-conditions). 전체 값을 보내면 서버가 그대로 바꾼다.
 * 그래서 null인 칸도 빼지 않고 null로 보낸다(기본값을 두지 않았다).
 *
 * @property regionCodes 시군구 코드 5자리 1~10개(예: "11440" 마포구)
 * @property priceMin 최소 가격(만원). 최소가 최대보다 크면 C001
 * @property types "APT"(아파트)·"OFFICETEL"(오피스텔) 중 1개 이상
 * @property areaMinM2 최소 전용면적(㎡)
 */
@Serializable
data class InterestConditionRequest(
    val regionCodes: List<String>,
    val priceMin: Int?,
    val priceMax: Int?,
    val types: List<String>,
    val areaMinM2: Int?,
    val areaMaxM2: Int?,
    val alertEnabled: Boolean
)

/** 저장된 관심 조건(조회·저장의 data). 조건을 정한 적이 없으면 조회의 data가 비어 있다 */
@Serializable
data class InterestConditionResponse(
    val regionCodes: List<String> = emptyList(),
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val types: List<String> = emptyList(),
    val areaMinM2: Int? = null,
    val areaMaxM2: Int? = null,
    val alertEnabled: Boolean = false
)
