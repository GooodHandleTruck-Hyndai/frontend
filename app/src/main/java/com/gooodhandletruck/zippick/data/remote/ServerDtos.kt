package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.model.LocalDateSerializer
import com.gooodhandletruck.zippick.data.model.OffsetDateTimeSerializer
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/*
 * 백엔드 응답 data의 모양. 필드 이름은 백엔드 DTO(Swagger)와 같다. 앱 화면은 이 클래스를 직접 쓰지 않고,
 * ServerMapping.kt에서 기존 데이터 클래스(ComplexSummary·ComplexDetail …)로 바꿔 쓴다.
 * 서버가 필드를 더해도 깨지지 않게 AppJson은 모르는 필드를 건너뛴다.
 */

/** POST /api/v1/devices (1번) */
@Serializable
data class DeviceDto(val anonymousId: String)

/** GET /api/v1/buildings/nearby (A 2번) */
@Serializable
data class NearbyBuildingsDto(
    @Serializable(with = LocalDateSerializer::class)
    val dataBaseDate: LocalDate? = null,
    val buildings: List<NearbyBuildingDto> = emptyList()
)

@Serializable
data class NearbyBuildingDto(
    val buildingId: Long,
    val name: String,
    val type: String,
    val address: String? = null,
    val lat: Double,
    val lon: Double,
    val distanceM: Int? = null,
    val imageUrl: String? = null,
    val latestDeal: DealDto? = null
)

/** 거래 한 건의 요약. 주변 건물의 latestDeal과 알림의 deal이 같은 모양이다 */
@Serializable
data class DealDto(
    val price: Int,
    @Serializable(with = LocalDateSerializer::class)
    val dealDate: LocalDate,
    val areaM2: Double,
    val areaPyeong: Double? = null
)

/** GET /api/v1/buildings/{buildingId} (A 7번) */
@Serializable
data class BuildingDetailDto(
    val buildingId: Long,
    val name: String,
    val type: String,
    val jibunAddress: String? = null,
    val roadAddress: String? = null,
    val builtYear: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val imageUrl: String? = null,
    val saved: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val dataBaseDate: LocalDate? = null,
    val recentTransactions: List<TransactionDto> = emptyList(),
    val hasMoreTransactions: Boolean = false
)

/** 실거래 한 건. 상세의 recentTransactions와 실거래 내역(A 8번)이 같은 모양이다 */
@Serializable
data class TransactionDto(
    @Serializable(with = LocalDateSerializer::class)
    val dealDate: LocalDate,
    val areaM2: Double,
    val floor: Int? = null,
    val dong: String? = null,
    val price: Int
)

/** GET /api/v1/buildings/{buildingId}/transactions (A 8번) */
@Serializable
data class TransactionListDto(
    val transactions: List<TransactionDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val hasNext: Boolean = false
)

/** GET /api/v1/favorites 한 칸 (B 10번) */
@Serializable
data class FavoriteDto(
    val buildingId: Long,
    val name: String,
    val type: String,
    val jibunAddress: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val savedAt: OffsetDateTime? = null
)

/** POST /api/v1/favorites 본문 (B 9번) */
@Serializable
data class FavoriteCreateDto(val buildingId: Long)

/** GET /api/v1/regions (B 2번). 실거래가 API의 시군구 코드 5자리만 쓴다 */
@Serializable
data class RegionDto(val lawdCd: String, val addressName: String? = null)

/**
 * GET·PUT /api/v1/interest-conditions (B 15·16번). 가격은 만원, 면적은 ㎡, 범위의 한쪽이 없으면 null.
 * PUT은 전체 값을 보내면 그대로 바꾼다.
 */
@Serializable
data class InterestConditionDto(
    val regionCodes: List<String>,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val types: List<String>,
    val areaMinM2: Int? = null,
    val areaMaxM2: Int? = null,
    val alertEnabled: Boolean
)

/** GET /api/v1/alerts (B 18번). 최신순, since가 없으면 최근 7일 */
@Serializable
data class AlertListDto(val alerts: List<AlertDto> = emptyList())

@Serializable
data class AlertDto(
    val alertId: Long,
    val buildingId: Long,
    val buildingName: String,
    val deal: DealDto,
    val read: Boolean = false,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null
)
