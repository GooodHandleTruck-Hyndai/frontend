package com.gooodhandletruck.zippick.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.zippick.data.model.ComplexDetail
import com.gooodhandletruck.zippick.data.model.ComplexSummary
import com.gooodhandletruck.zippick.data.model.HeardComplex
import com.gooodhandletruck.zippick.data.model.HeardResponse
import com.gooodhandletruck.zippick.data.model.NearbyResponse
import com.gooodhandletruck.zippick.data.model.OffsetDateTimeSerializer
import com.gooodhandletruck.zippick.data.model.SavedComplex
import com.gooodhandletruck.zippick.data.model.SavedResponse
import com.gooodhandletruck.zippick.data.remote.BuildingDetailDto
import com.gooodhandletruck.zippick.data.remote.FavoriteCreateDto
import com.gooodhandletruck.zippick.data.remote.FavoriteDto
import com.gooodhandletruck.zippick.data.remote.NearbyBuildingsDto
import com.gooodhandletruck.zippick.data.remote.PERIOD_DAYS
import com.gooodhandletruck.zippick.data.remote.ServerClient
import com.gooodhandletruck.zippick.data.remote.ServerException
import com.gooodhandletruck.zippick.data.remote.TransactionDto
import com.gooodhandletruck.zippick.data.remote.TransactionListDto
import com.gooodhandletruck.zippick.data.remote.decode
import com.gooodhandletruck.zippick.data.remote.reachesBefore
import com.gooodhandletruck.zippick.data.remote.toDetail
import com.gooodhandletruck.zippick.data.remote.toSummary
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * 백엔드를 부르는 단지 저장소. 화면은 [FakeRepository] 때와 같은 모양을 받는다(변환은 remote/ServerMapping.kt).
 *
 * 서버에 있는 것: 주변 건물·건물 상세·실거래 내역(A), 저장하기·저장 목록·저장 취소(B).
 * 서버에 아직 없는 것은 앱에서 대신한다.
 * - 앞쪽 단지(A 20번 미완): 주변 건물을 받아 차가 향한 쪽 180도 안에서 가장 가까운 단지를 앱이 고른다
 * - geofences: 주변 건물(최대 3km)을 받아 알림 조건에 맞는 단지를 앱이 거른다
 * - 들은 기록(B 21·22번 미완): 이 기기(SharedPreferences)에 기억한다
 *   TODO: 주행 중 질의 기록 API가 생기면 recordHeard·heardToday를 서버로 바꾼다
 */
class RemoteRepository(context: Context, private val server: ServerClient) : ComplexRepository {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 최근에 받은 단지 카드. 저장 목록·들은 기록에 가격과 좌표를 채우고, 상세의 좌표가 없을 때 쓴다 */
    private val summaries = ConcurrentHashMap<String, ComplexSummary>()

    /** 저장한 단지 ID. 처음 필요할 때 저장 목록을 한 번 받아 오고, 저장·취소할 때 고친다 */
    @Volatile
    private var savedIds: Set<String>? = null
    private val savedLock = Mutex()

    private val heardLock = Mutex()

    override suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse {
        val data = server.get(
            NEARBY_PATH,
            mapOf("lat" to lat, "lon" to lon, "radius" to radiusMeters.coerceIn(MIN_RADIUS, MAX_RADIUS))
        )
        val response = data.decode<NearbyBuildingsDto>()
        val saved = savedIds()
        val items = response.buildings.map { it.toSummary(response.dataBaseDate, it.buildingId.toString() in saved) }
        items.forEach { summaries[it.complexId] = it }
        return NearbyResponse(totalCount = items.size, items = items.take(limit))
    }

    override suspend fun detail(complexId: String): ComplexDetail {
        val buildingId = complexId.toBuildingId()
        val dto = getBuilding(buildingId)
        val trades = dto.recentTransactions + moreTrades(buildingId, dto)
        return dto.toDetail(
            trades = trades,
            saved = complexId in savedIds(),
            fallback = summaries[complexId],
            today = LocalDate.now(SERVER_ZONE)
        )
    }

    /** 이미 저장했거나(F001) 이미 없어도(F002) 성공으로 본다. 다시 보내도 결과가 같다 */
    override suspend fun setSaved(complexId: String, saved: Boolean) {
        val buildingId = complexId.toBuildingId()
        try {
            if (saved) {
                val body = AppJson.encodeToJsonElement(FavoriteCreateDto.serializer(), FavoriteCreateDto(buildingId))
                server.post(FAVORITES_PATH, body)
            } else {
                server.delete("$FAVORITES_PATH/$buildingId")
            }
        } catch (e: ServerException) {
            val alreadyDone = (saved && e.code == CODE_ALREADY_SAVED) || (!saved && e.code == CODE_NOT_SAVED)
            if (!alreadyDone) throw e
        }
        savedLock.withLock { savedIds = savedIds?.let { if (saved) it + complexId else it - complexId } }
        summaries.computeIfPresent(complexId) { _, summary -> summary.copy(saved = saved) }
    }

    /** 저장 목록. 서버 목록에는 가격이 없어서, 최근에 본 카드가 없는 단지만 상세를 불러 가격을 채운다 */
    override suspend fun saved(): SavedResponse {
        val favorites = server.get(FAVORITES_PATH).decode<List<FavoriteDto>>()
        savedLock.withLock { savedIds = favorites.map { it.buildingId.toString() }.toSet() }
        val items = coroutineScope {
            favorites.map { favorite ->
                async { SavedComplex(savedAt = favorite.savedAt ?: now(), complex = summaryOf(favorite)) }
            }.awaitAll()
        }
        return SavedResponse(items)
    }

    override suspend fun brief(lat: Double, lon: Double, heading: Double): ComplexSummary? =
        nearby(lat, lon, BRIEF_RADIUS_METERS, limit = Int.MAX_VALUE)
            .nearestAhead(lat, lon, heading, BRIEF_RADIUS_METERS)

    override suspend fun recordHeard(complexId: String) = heardLock.withLock {
        val today = startOfToday()
        val entry = HeardEntry(complexId, now(), summaries[complexId])
        writeHeard(listOf(entry) + readHeard().filter { it.heardAt >= today })
    }

    override suspend fun heardToday(): HeardResponse {
        val entries = heardLock.withLock { readHeard() }
        val saved = savedIds()
        val today = startOfToday()
        return HeardResponse(
            items = entries
                .filter { it.heardAt >= today }
                .sortedByDescending { it.heardAt }
                .distinctBy { it.complexId }
                .mapNotNull { entry ->
                    (summaries[entry.complexId] ?: entry.complex)?.let { complex ->
                        HeardComplex(entry.heardAt, complex.copy(saved = entry.complexId in saved))
                    }
                }
        )
    }

    override suspend fun geofences(lat: Double, lon: Double, conditions: AlertConditions): List<ComplexSummary> =
        nearby(lat, lon, MAX_RADIUS, limit = Int.MAX_VALUE).items.filter { conditions.matches(it) }

    private suspend fun getBuilding(buildingId: Long): BuildingDetailDto = try {
        server.get("$BUILDINGS_PATH/$buildingId").decode()
    } catch (e: ServerException) {
        if (e.code == CODE_BUILDING_NOT_FOUND) throw ComplexNotFoundException(buildingId.toString())
        throw e
    }

    /** 평균 기간(90일)보다 오래된 거래가 나올 때까지 실거래 내역을 더 받는다. 너무 많이 부르지 않게 쪽 수를 묶는다 */
    private suspend fun moreTrades(buildingId: Long, dto: BuildingDetailDto): List<TransactionDto> {
        if (!dto.hasMoreTransactions) return emptyList()
        val since = (dto.dataBaseDate ?: LocalDate.now(SERVER_ZONE)).minusDays(PERIOD_DAYS.toLong())
        val trades = mutableListOf<TransactionDto>()
        for (page in 0 until MAX_TRADE_PAGES) {
            val list = server.get(
                "$BUILDINGS_PATH/$buildingId/transactions",
                mapOf("page" to page, "size" to TRADE_PAGE_SIZE)
            ).decode<TransactionListDto>()
            trades += list.transactions
            if (!list.hasNext || list.transactions.reachesBefore(since)) break
        }
        return trades
    }

    /** 최근에 본 카드 → 상세 한 번 → 저장 목록 칸 순으로 카드를 만든다 */
    private suspend fun summaryOf(favorite: FavoriteDto): ComplexSummary {
        val complexId = favorite.buildingId.toString()
        summaries[complexId]?.let { return it.copy(saved = true) }
        return try {
            getBuilding(favorite.buildingId)
                .toSummary(saved = true, fallbackLat = favorite.lat, fallbackLon = favorite.lon)
                .also { summaries[complexId] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "저장한 단지 $complexId 의 가격을 불러오지 못했어요", e)
            favorite.toSummary()
        }
    }

    /** 저장한 단지 ID. 저장 목록을 못 받아도 주변·상세는 보여야 하니 그때는 빈 집합으로 본다 */
    private suspend fun savedIds(): Set<String> {
        savedIds?.let { return it }
        return try {
            val favorites = server.get(FAVORITES_PATH).decode<List<FavoriteDto>>()
            favorites.map { it.buildingId.toString() }.toSet().also { ids -> savedLock.withLock { savedIds = ids } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "저장 목록을 받지 못해 저장 표시 없이 보여 줘요", e)
            emptySet()
        }
    }

    /** 서버 ID는 숫자다. 목업 시절 ID("c-1001")처럼 숫자가 아니면 서버에 없는 단지로 본다 */
    private fun String.toBuildingId(): Long = toLongOrNull() ?: throw ComplexNotFoundException(this)

    private suspend fun readHeard(): List<HeardEntry> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_HEARD, null) ?: return@withContext emptyList()
        try {
            AppJson.decodeFromString(HEARD_SERIALIZER, json)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "들은 기록을 읽지 못해 비웠어요", e)
            emptyList()
        }
    }

    private suspend fun writeHeard(entries: List<HeardEntry>) = withContext(Dispatchers.IO) {
        prefs.edit { putString(KEY_HEARD, AppJson.encodeToString(HEARD_SERIALIZER, entries)) }
    }

    private fun now(): OffsetDateTime = OffsetDateTime.now(SERVER_ZONE).truncatedTo(ChronoUnit.SECONDS)

    private fun startOfToday(): OffsetDateTime = LocalDate.now(SERVER_ZONE).atStartOfDay(SERVER_ZONE).toOffsetDateTime()

    private companion object {
        const val TAG = "RemoteRepository"
        const val NEARBY_PATH = "api/v1/buildings/nearby"
        const val BUILDINGS_PATH = "api/v1/buildings"
        const val FAVORITES_PATH = "api/v1/favorites"
        const val CODE_BUILDING_NOT_FOUND = "B001"
        const val CODE_ALREADY_SAVED = "F001"
        const val CODE_NOT_SAVED = "F002"

        /** 서버 주변 건물 반경 범위(m) */
        const val MIN_RADIUS = 100
        const val MAX_RADIUS = 3000
        const val BRIEF_RADIUS_METERS = 1000
        const val TRADE_PAGE_SIZE = 50
        const val MAX_TRADE_PAGES = 4
        const val PREFS_NAME = "remote_local"
        const val KEY_HEARD = "heard"
        val SERVER_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        val HEARD_SERIALIZER = ListSerializer(HeardEntry.serializer())
    }
}

/** 이 기기에 기억하는 들은 기록 한 건. 그때의 카드를 같이 둬서, 앱을 다시 켜도 S-06에 그릴 수 있다 */
@Serializable
internal data class HeardEntry(
    val complexId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val heardAt: OffsetDateTime,
    val complex: ComplexSummary? = null
)
