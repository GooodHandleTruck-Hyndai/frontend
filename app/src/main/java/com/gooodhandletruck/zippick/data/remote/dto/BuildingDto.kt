package com.gooodhandletruck.zippick.data.remote.dto

import com.gooodhandletruck.zippick.data.model.LocalDateSerializer
import java.time.LocalDate
import kotlinx.serialization.Serializable

/*
 * 건물 API의 응답 data. 이름과 모양은 백엔드 domain/building/dto·domain/trade/dto와 같다.
 * 서버는 단지를 '건물(building)'이라 부르고 buildingId는 숫자다. 금액은 만원 단위 정수(59700 = 5억 9,700만 원),
 * 날짜는 "2026-09-30" 형식이다. 좌표 이름은 모든 API에서 lat·lon이다.
 * 지금 서버에는 서울 마포구 아파트·오피스텔 실거래만 있다.
 */

/**
 * 주변 건물(GET /api/v1/buildings/nearby).
 *
 * @property dataBaseDate 자료 기준일. 서버가 국토부 자료를 마지막으로 받은 날(목록에 나온 구 중 가장 오래된 날). 목록이 비면 null
 * @property buildings 반경 안의 건물, 가까운 순. 없으면 빈 목록
 */
@Serializable
data class NearbyBuildingsResponse(
    @Serializable(with = LocalDateSerializer::class)
    val dataBaseDate: LocalDate? = null,
    val buildings: List<NearbyBuilding> = emptyList()
)

/**
 * 주변 건물 한 곳. [buildingId]를 건물 상세에 그대로 넣는다.
 *
 * @property type "APT"(아파트) 또는 "OFFICETEL"(오피스텔)
 * @property address 지번 주소(시도는 줄임말, 예: "서울 마포구 도화동 555")
 * @property distanceM 요청한 위치에서 건물까지 거리(m, 반올림)
 * @property imageUrl 사진 출처가 정해지기 전까지 늘 null
 * @property latestDeal 최근 1년 거래 중 가장 최근 한 건(해제 거래 제외). 없으면 null
 */
@Serializable
data class NearbyBuilding(
    val buildingId: Long,
    val name: String,
    val type: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val distanceM: Int,
    val imageUrl: String? = null,
    val latestDeal: LatestDeal? = null
)

/**
 * 주변 건물의 최근 거래 한 건.
 *
 * @property price 거래금액(만원)
 * @property areaM2 전용면적 ㎡(소수 2자리)
 * @property areaPyeong 전용면적 평(소수 1자리, 1평 = 3.3058㎡)
 */
@Serializable
data class LatestDeal(
    val price: Int,
    @Serializable(with = LocalDateSerializer::class)
    val dealDate: LocalDate,
    val areaM2: Double,
    val areaPyeong: Double
)

/**
 * 건물 상세(GET /api/v1/buildings/{buildingId}).
 *
 * @property jibunAddress 지번 주소(시도는 줄임말)
 * @property roadAddress 도로명 주소. 서버가 아직 준비 중이라 지금은 늘 null
 * @property lat 주소로 좌표를 못 찾으면 null
 * @property saved 내가 저장한 건물인지. X-Anonymous-Id를 보내야 맞는 값이 온다
 * @property dataBaseDate 자료 기준일. 그 구 자료를 한 번도 받지 않았으면 null
 * @property recentTransactions 최근 거래 최대 3건, 최신순. 해제(취소)된 거래는 빠진다
 * @property hasMoreTransactions true면 [더보기]를 보여 주고 실거래 내역 API를 부른다
 */
@Serializable
data class BuildingDetailResponse(
    val buildingId: Long,
    val name: String,
    val type: String,
    val jibunAddress: String,
    val roadAddress: String? = null,
    val builtYear: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val imageUrl: String? = null,
    val saved: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val dataBaseDate: LocalDate? = null,
    val recentTransactions: List<TransactionResponse> = emptyList(),
    val hasMoreTransactions: Boolean = false
)

/**
 * 실거래 한 건. 건물 상세의 recentTransactions와 실거래 내역이 같은 모양을 쓴다.
 *
 * @property areaM2 전용면적 ㎡(소수 2자리)
 * @property areaPyeong 전용면적 평(소수 1자리)
 * @property dong 동 번호("동" 글자 없이, 예: "101"). 자료에 없으면 null
 * @property price 거래금액(만원)
 */
@Serializable
data class TransactionResponse(
    @Serializable(with = LocalDateSerializer::class)
    val dealDate: LocalDate,
    val areaM2: Double,
    val areaPyeong: Double,
    val floor: Int,
    val dong: String? = null,
    val price: Int
)

/**
 * 실거래 내역(더보기, GET /api/v1/buildings/{buildingId}/transactions).
 *
 * @property transactions 이번 쪽 거래, 최신순
 * @property page 요청한 쪽 번호(0부터)
 * @property hasNext true일 때만 page + 1로 다음 쪽을 부른다
 */
@Serializable
data class TransactionListResponse(
    val transactions: List<TransactionResponse> = emptyList(),
    val page: Int,
    val size: Int,
    val hasNext: Boolean
)

/** 가격 추이(GET /api/v1/buildings/{buildingId}/price-trend). 이번 달 포함 최근 12개월, 오래된 달부터 늘 12개 */
@Serializable
data class PriceTrendResponse(val months: List<MonthlyPrice> = emptyList())

/**
 * 한 달 평균가.
 *
 * @property yearMonth 계약월("2025-12")
 * @property avgPrice 그 달 평균 거래가(만원, 반올림). 거래가 없는 달은 null이고, 그래프 선을 끊는다
 * @property count 그 달 거래 수(해제 거래 제외)
 */
@Serializable
data class MonthlyPrice(
    val yearMonth: String,
    val avgPrice: Int? = null,
    val count: Int
)
