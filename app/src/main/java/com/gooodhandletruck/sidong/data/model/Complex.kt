package com.gooodhandletruck.sidong.data.model

import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/*
 * 서버 응답과 같은 모양의 데이터 클래스. 형식은 Claude Docs 'API 명세 초안 검토'의 P0 제안을 따른다.
 * 금액은 만원 단위 정수(priceManwon), 좌표는 lat·lon, 날짜는 "2026-10-31" 형식이다.
 */

/** 주변 단지 응답(S-03). [items]는 가까운 순이고, [totalCount]는 반경 안 전체 수다. */
@Serializable
data class NearbyResponse(
    val totalCount: Int,
    val items: List<ComplexSummary>
)

/**
 * 단지 카드 한 장에 필요한 값. 주변 단지·저장 목록·오늘 들은 단지가 같은 모양을 쓴다.
 * 거리와 방향은 서버가 주지 않고, 앱이 lat·lon과 차의 heading으로 계산한다.
 *
 * @property type "APT"(아파트). 오피스텔·원룸은 P0 다음에 더한다
 * @property address 도로명주소. 길 안내(NaviHelper RouteInfo)에 그대로 쓴다
 * @property imageUrl 없으면 null이고, 앱은 이름 첫 글자를 넣은 회색 칸을 그린다
 * @property price 거래 기록이 아예 없으면 null
 */
@Serializable
data class ComplexSummary(
    val complexId: String,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val imageUrl: String? = null,
    val saved: Boolean = false,
    val price: RepresentativePrice? = null
)

/**
 * 대표 평형의 가격. 대표 평형은 최근 [periodDays]일 거래가 가장 많은 평형이고,
 * 그 기간 거래가 없으면 마지막으로 거래된 평형이다.
 *
 * @property areaM2 전용면적(㎡)
 * @property avgPriceManwon 최근 [periodDays]일 평균. 그 기간 거래가 없으면 null이고 [lastTrade]를 보여 준다
 * @property asOf 자료 기준일
 */
@Serializable
data class RepresentativePrice(
    val areaM2: Double,
    val avgPriceManwon: Int? = null,
    val tradeCount: Int,
    val periodDays: Int,
    @Serializable(with = LocalDateSerializer::class)
    val asOf: LocalDate,
    val lastTrade: Trade? = null
)

/** 실거래 한 건 */
@Serializable
data class Trade(
    @Serializable(with = LocalDateSerializer::class)
    val contractDate: LocalDate,
    val floor: Int,
    val priceManwon: Int
)

/**
 * 단지 상세 응답(S-04). 길 안내(NaviHelper RouteInfo)에는 lat·lon·name·address를 그대로 쓴다.
 *
 * @property saved 요청에 X-Device-Id를 보내야 맞는 값이 온다
 * @property periodDays 평균을 내는 기간(일). 90이면 화면에 "최근 3개월"로 쓴다
 * @property defaultAreaM2 카드의 대표 평형이자 S-04가 처음 고르는 평형 칩. 거래 기록이 아예 없으면 null
 * @property areas 거래가 있는 평형만 면적 순으로 담는다. 거래 기록이 아예 없으면 비어 있다
 */
@Serializable
data class ComplexDetail(
    val complexId: String,
    val type: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val imageUrl: String? = null,
    val saved: Boolean = false,
    val periodDays: Int,
    @Serializable(with = LocalDateSerializer::class)
    val asOf: LocalDate,
    val defaultAreaM2: Double? = null,
    val areas: List<AreaPrice> = emptyList()
)

/**
 * 평형 하나의 가격.
 *
 * @property avgPriceManwon 최근 periodDays일 평균. 그 기간 거래가 없으면 null이고 [recentTrades] 첫 건이 마지막 거래다
 * @property tradeCount 최근 periodDays일 거래 수
 * @property officialPriceManwon 공시가격. 가져올 곳이 정해질 때까지 null일 수 있다 (API 검토 '고칠 것' 2번)
 * @property recentTrades 기간과 상관없이 최근 거래 최대 3건, 최신 순
 */
@Serializable
data class AreaPrice(
    val areaM2: Double,
    val avgPriceManwon: Int? = null,
    val tradeCount: Int,
    val officialPriceManwon: Int? = null,
    val recentTrades: List<Trade> = emptyList()
)

/** 저장 목록 응답(S-05). 최근에 저장한 것부터 온다 */
@Serializable
data class SavedResponse(val items: List<SavedComplex>)

/** 저장한 단지 하나. [complex]는 주변 단지 응답의 items 하나와 같은 모양이라 같은 카드로 그린다 */
@Serializable
data class SavedComplex(
    @Serializable(with = OffsetDateTimeSerializer::class)
    val savedAt: OffsetDateTime,
    val complex: ComplexSummary
)

/**
 * 오늘 들은 단지 응답(S-06). 오늘 0시(+09:00)부터 주행 중에 들은 단지를 최근 것부터 준다.
 * 같은 단지를 여러 번 들었으면 마지막으로 들은 시각 하나만 온다. 저장 목록과 같은 모양에 savedAt 대신 heardAt이다.
 */
@Serializable
data class HeardResponse(val items: List<HeardComplex>)

@Serializable
data class HeardComplex(
    @Serializable(with = OffsetDateTimeSerializer::class)
    val heardAt: OffsetDateTime,
    val complex: ComplexSummary
)

/** S-04가 처음 고를 평형: 서버가 정한 대표 평형. 목록에 없으면 최근 거래가 가장 많은 평형이다 */
val ComplexDetail.defaultArea: AreaPrice?
    get() = areas.firstOrNull { it.areaM2 == defaultAreaM2 } ?: areas.maxByOrNull { it.tradeCount }
