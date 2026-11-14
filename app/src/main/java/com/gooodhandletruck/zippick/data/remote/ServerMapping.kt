package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.AlertConditions
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.model.AreaPrice
import com.gooodhandletruck.zippick.data.model.ComplexDetail
import com.gooodhandletruck.zippick.data.model.ComplexSummary
import com.gooodhandletruck.zippick.data.model.RepresentativePrice
import com.gooodhandletruck.zippick.data.model.Trade
import com.gooodhandletruck.zippick.data.model.UNKNOWN_COORDINATE
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.roundToInt

/*
 * 서버 응답 → 앱 데이터 클래스. 화면은 목업 시절 모양(ComplexSummary·ComplexDetail)을 그대로 쓰고,
 * 서버에 없는 값은 여기서 만든다. 단지 ID는 서버의 숫자 buildingId를 문자열로 바꿔 complexId에 넣는다.
 */

/** 평균을 내는 기간(일). 화면에 "최근 3개월"로 쓴다 */
internal const val PERIOD_DAYS = 90

/**
 * 주변 건물 한 곳 → 카드. 서버는 최근 1년 거래 1건만 주므로 평균은 비우고 그 거래를 마지막 거래로 보여 준다.
 * 평균·평형별 가격은 상세(S-04)에서 거래 내역으로 계산한다.
 */
internal fun NearbyBuildingDto.toSummary(asOf: LocalDate?, saved: Boolean) = ComplexSummary(
    complexId = buildingId.toString(),
    type = type,
    name = name,
    address = address.orEmpty(),
    lat = lat,
    lon = lon,
    imageUrl = imageUrl,
    saved = saved,
    price = latestDeal?.let { deal ->
        RepresentativePrice(
            areaM2 = deal.areaM2,
            avgPriceManwon = null,
            tradeCount = 0,
            periodDays = PERIOD_DAYS,
            asOf = asOf ?: deal.dealDate,
            lastTrade = Trade(contractDate = deal.dealDate, floor = null, priceManwon = deal.price)
        )
    }
)

/** 상세 응답 → 카드(저장 목록에 처음 보는 단지가 있을 때). 가장 최근 거래를 마지막 거래로 쓴다 */
internal fun BuildingDetailDto.toSummary(saved: Boolean, fallbackLat: Double?, fallbackLon: Double?) = ComplexSummary(
    complexId = buildingId.toString(),
    type = type,
    name = name,
    address = roadAddress ?: jibunAddress.orEmpty(),
    lat = lat ?: fallbackLat ?: UNKNOWN_COORDINATE,
    lon = lon ?: fallbackLon ?: UNKNOWN_COORDINATE,
    imageUrl = imageUrl,
    saved = saved,
    price = recentTransactions.maxByOrNull { it.dealDate }?.let { trade ->
        RepresentativePrice(
            areaM2 = trade.areaM2,
            avgPriceManwon = null,
            tradeCount = 0,
            periodDays = PERIOD_DAYS,
            asOf = dataBaseDate ?: trade.dealDate,
            lastTrade = trade.toTrade()
        )
    }
)

/** 저장 목록 한 칸 → 카드. 상세도 못 불렀을 때 쓰는 최소 모양이라 가격이 없다 */
internal fun FavoriteDto.toSummary() = ComplexSummary(
    complexId = buildingId.toString(),
    type = type,
    name = name,
    address = jibunAddress.orEmpty(),
    lat = lat ?: UNKNOWN_COORDINATE,
    lon = lon ?: UNKNOWN_COORDINATE,
    saved = true
)

/**
 * 상세 + 거래 내역 → S-04. [trades]는 [detailTrades]로 고른 거래 목록이다(같은 거래가 두 번 들어 있지 않다).
 * - 평형은 전용면적(소수 2자리)이 같은 거래끼리 묶는다
 * - 평형마다 기준일부터 [PERIOD_DAYS]일 안의 평균·건수, 기간과 상관없는 최근 3건
 * - 처음 고를 평형은 기간 안 거래가 가장 많은 평형, 없으면 가장 최근에 거래된 평형
 * 공시가격은 서버에 아직 없어 null이다.
 * 좌표를 못 찾은 건물은 [fallback](주변 목록에서 본 값)으로, 그것도 없으면 [UNKNOWN_COORDINATE]로 둔다(길 안내를 막는다).
 */
internal fun BuildingDetailDto.toDetail(
    trades: List<TransactionDto>,
    saved: Boolean,
    fallback: ComplexSummary?,
    today: LocalDate
): ComplexDetail {
    val asOf = dataBaseDate ?: today
    val since = asOf.minusDays(PERIOD_DAYS.toLong())
    val unique = trades.sortedByDescending { it.dealDate }
    val areas = unique
        .groupBy { it.areaM2.roundArea() }
        .map { (area, list) ->
            val inPeriod = list.filter { !it.dealDate.isBefore(since) }
            AreaPrice(
                areaM2 = area,
                avgPriceManwon = inPeriod.takeIf { it.isNotEmpty() }?.map { it.price }?.average()?.roundToInt(),
                tradeCount = inPeriod.size,
                officialPriceManwon = null,
                recentTrades = list.take(RECENT_TRADES).map { it.toTrade() }
            )
        }
        .sortedBy { it.areaM2 }
    val defaultArea = areas.filter { it.tradeCount > 0 }.maxByOrNull { it.tradeCount }?.areaM2
        ?: unique.firstOrNull()?.areaM2?.roundArea()
    return ComplexDetail(
        complexId = buildingId.toString(),
        type = type,
        name = name,
        address = roadAddress ?: jibunAddress ?: fallback?.address.orEmpty(),
        lat = lat ?: fallback?.lat ?: UNKNOWN_COORDINATE,
        lon = lon ?: fallback?.lon ?: UNKNOWN_COORDINATE,
        imageUrl = imageUrl,
        saved = saved,
        periodDays = PERIOD_DAYS,
        asOf = asOf,
        defaultAreaM2 = defaultArea,
        areas = areas
    )
}

/**
 * 상세에 쓸 거래 목록. 상세의 최근 3건([recent])은 실거래 내역 첫 쪽([more])과 같은 거래라,
 * 더 불러왔으면 내역만 쓰고 아니면 최근 3건만 쓴다. 둘을 합치면 같은 거래가 두 번 세어진다.
 */
internal fun detailTrades(recent: List<TransactionDto>, more: List<TransactionDto>): List<TransactionDto> =
    more.ifEmpty { recent }

/** 거래 내역을 더 불러와야 하는지: 가장 오래된 거래가 아직 평균 기간 안이면 더 본다 */
internal fun List<TransactionDto>.reachesBefore(date: LocalDate): Boolean = any { it.dealDate.isBefore(date) }

private const val RECENT_TRADES = 3

private fun Double.roundArea(): Double = (this * 100).roundToInt() / 100.0

private fun TransactionDto.toTrade() = Trade(contractDate = dealDate, floor = floor, priceManwon = price)

/**
 * S-02 알림 조건 → 서버 관심 조건. 앱은 가격대·면적을 여러 구간 고를 수 있지만 서버는 범위 하나만 받으므로,
 * 고른 구간의 가장 낮은 끝부터 가장 높은 끝까지로 보낸다(끝이 열린 구간을 골랐으면 그쪽은 null).
 * 유형은 서버가 아는 값(APT·OFFICETEL)만 보내고, 하나도 없으면 APT를 보낸다.
 */
internal fun AlertConditions.toServer(regionCode: String, alertEnabled: Boolean): InterestConditionDto {
    val serverTypes = types.map { it.name }.filter { it in SERVER_TYPES }.ifEmpty { listOf(ComplexType.APT.name) }
    return InterestConditionDto(
        regionCodes = listOf(regionCode),
        priceMin = prices.lowerBound { it.minManwon },
        priceMax = prices.upperBound { it.maxManwon },
        types = serverTypes.sorted(),
        areaMinM2 = areas.lowerBound { it.minM2 },
        areaMaxM2 = areas.upperBound { it.maxM2 },
        alertEnabled = alertEnabled
    )
}

private val SERVER_TYPES = setOf("APT", "OFFICETEL")

private inline fun <T> Collection<T>.lowerBound(min: (T) -> Int?): Int? =
    if (isEmpty() || any { min(it) == null }) null else mapNotNull(min).minOrNull()

private inline fun <T> Collection<T>.upperBound(max: (T) -> Int?): Int? =
    if (isEmpty() || any { max(it) == null }) null else mapNotNull(max).maxOrNull()

/** 관심 건물에 새 거래가 생겼다는 알림 한 건(S-10) */
data class NewTradeAlert(
    val alertId: Long,
    val complexId: String,
    val name: String,
    val priceManwon: Int,
    val contractDate: LocalDate,
    val areaM2: Double,
    val read: Boolean,
    val createdAt: OffsetDateTime?
)

internal fun AlertDto.toAlert() = NewTradeAlert(
    alertId = alertId,
    complexId = buildingId.toString(),
    name = buildingName,
    priceManwon = deal.price,
    contractDate = deal.dealDate,
    areaM2 = deal.areaM2,
    read = read,
    createdAt = createdAt
)
