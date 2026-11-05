package com.gooodhandletruck.sidong.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import com.gooodhandletruck.sidong.data.model.HeardComplex
import com.gooodhandletruck.sidong.data.model.HeardResponse
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.data.model.OffsetDateTimeSerializer
import com.gooodhandletruck.sidong.data.model.SavedComplex
import com.gooodhandletruck.sidong.data.model.SavedResponse
import java.time.DateTimeException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/**
 * 서버 전에 쓰는 단지 저장소. app/src/main/assets/mock/의 JSON을 읽는다. 단지 이름·주소·가격은 가상 데이터다.
 * - 주변 단지: 시연 지역(강남역 둘레)의 단지를 한 파일에 모두 넣어 두고, 요청한 위치·반경으로 걸러 서버처럼 답한다.
 * - 단지 상세: 상세 응답을 단지마다 하나씩 배열로 넣어 두고 complexId로 찾는다.
 * - 저장·들은 기록: 서버 대신 이 기기(SharedPreferences)에 기억해서, 앱을 다시 켜도 남는다. 앱 데이터를 지우면 사라진다.
 */
class FakeRepository(context: Context) : ComplexRepository {

    private val assets = context.applicationContext.assets
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 처음 부를 때 한 번만 읽는다
    private val allNearby: NearbyResponse by lazy { read(NEARBY_FILE) }
    private val details: Map<String, ComplexDetail> by lazy {
        read<List<ComplexDetail>>(DETAILS_FILE).associateBy { it.complexId }
    }

    private val savedEntries = StoredList(prefs, KEY_SAVED, FakeSavedEntry.serializer())
    private val heardEntries = StoredList(prefs, KEY_HEARD, FakeHeardEntry.serializer())

    override suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse =
        withContext(Dispatchers.IO) {
            delay(FAKE_LATENCY_MS) // 실제 통신처럼 '불러오는 중' 화면이 잠깐 보이게 한다
            val response = allNearby.within(lat, lon, radiusMeters, limit)
            val savedIds = savedIds()
            response.copy(items = response.items.map { it.copy(saved = it.complexId in savedIds) })
        }

    override suspend fun detail(complexId: String): ComplexDetail = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        val detail = details[complexId] ?: throw ComplexNotFoundException(complexId)
        detail.copy(saved = complexId in savedIds())
    }

    override suspend fun setSaved(complexId: String, saved: Boolean) = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        savedEntries.update { entries ->
            when {
                // 이미 저장한 단지를 다시 저장해도 저장한 시각은 그대로다(PUT은 두 번 보내도 결과가 같다)
                saved && entries.any { it.complexId == complexId } -> entries
                saved -> listOf(FakeSavedEntry(complexId, now())) + entries
                else -> entries.filterNot { it.complexId == complexId }
            }
        }
    }

    override suspend fun saved(): SavedResponse = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        allNearby.savedList(savedEntries.get())
    }

    override suspend fun brief(lat: Double, lon: Double, heading: Double): ComplexSummary? =
        withContext(Dispatchers.IO) {
            delay(FAKE_LATENCY_MS)
            val complex = allNearby.nearestAhead(lat, lon, heading, BRIEF_RADIUS_METERS)
            complex?.copy(saved = complex.complexId in savedIds())
        }

    override suspend fun recordHeard(complexId: String) = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        val today = startOfToday()
        // 어제까지의 기록은 S-06에 나오지 않으니 쓸 때 지운다
        heardEntries.update { entries ->
            listOf(FakeHeardEntry(complexId, now())) + entries.filter { it.heardAt >= today }
        }
    }

    override suspend fun heardToday(): HeardResponse = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        allNearby.heardList(heardEntries.get(), since = startOfToday(), savedIds = savedIds())
    }

    override suspend fun geofences(lat: Double, lon: Double, conditions: AlertConditions): List<ComplexSummary> =
        withContext(Dispatchers.IO) {
            delay(FAKE_LATENCY_MS)
            allNearby.geofencesAround(lat, lon, GEOFENCE_RADIUS_METERS, conditions)
        }

    private fun savedIds(): Set<String> = savedEntries.get().map { it.complexId }.toSet()

    private fun now(): OffsetDateTime = OffsetDateTime.now(SERVER_ZONE).truncatedTo(ChronoUnit.SECONDS)

    private fun startOfToday(): OffsetDateTime = LocalDate.now(SERVER_ZONE).atStartOfDay(SERVER_ZONE).toOffsetDateTime()

    private inline fun <reified T> read(path: String): T =
        assets.open(path).bufferedReader().use { AppJson.decodeFromString(it.readText()) }

    private companion object {
        const val NEARBY_FILE = "mock/complexes_nearby.json"
        const val DETAILS_FILE = "mock/complex_details.json"
        const val FAKE_LATENCY_MS = 400L
        const val BRIEF_RADIUS_METERS = 1000
        const val GEOFENCE_RADIUS_METERS = 5000
        const val PREFS_NAME = "fake_server"
        const val KEY_SAVED = "saved"
        const val KEY_HEARD = "heard"
        val SERVER_ZONE: ZoneId = ZoneId.of("Asia/Seoul") // 시각은 +09:00으로 주고받는다
    }
}

/**
 * 목업 서버가 기기(SharedPreferences)에 JSON으로 기억하는 목록. 처음 쓸 때 읽어 오고, 바뀔 때마다 쓴다.
 * 읽기와 쓰기를 이 객체 하나로 잠가서 여러 스레드에서 불러도 섞이지 않는다.
 */
private class StoredList<T>(
    private val prefs: SharedPreferences,
    private val key: String,
    elementSerializer: KSerializer<T>
) {
    private val serializer = ListSerializer(elementSerializer)
    private var cache: List<T>? = null

    @Synchronized
    fun get(): List<T> = cache ?: load().also { cache = it }

    @Synchronized
    fun update(change: (List<T>) -> List<T>) {
        val before = get()
        val after = change(before)
        if (after == before) return
        cache = after
        prefs.edit { putString(key, AppJson.encodeToString(serializer, after)) }
    }

    private fun load(): List<T> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            AppJson.decodeFromString(serializer, json)
        } catch (e: IllegalArgumentException) { // JSON이 깨졌을 때(SerializationException 포함)
            Log.w(TAG, "기기에 기억한 $key 목록을 읽지 못해 비웠어요", e)
            emptyList()
        } catch (e: DateTimeException) {
            Log.w(TAG, "기기에 기억한 $key 시각을 읽지 못해 비웠어요", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "FakeRepository"
    }
}

/** 목업 서버가 기억하는 저장 한 건 */
@Serializable
internal data class FakeSavedEntry(
    val complexId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val savedAt: OffsetDateTime
)

/** 목업 서버가 기억하는 들은 기록 한 건(HEARD) */
@Serializable
internal data class FakeHeardEntry(
    val complexId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val heardAt: OffsetDateTime
)

/** 서버의 주변 단지 규칙: (lat, lon)에서 반경 안의 단지를 가까운 순으로 limit개 주고, totalCount는 반경 안 전체 수다. */
internal fun NearbyResponse.within(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse {
    val inRange = items
        .map { complex -> complex to distanceMeters(lat, lon, complex.lat, complex.lon) }
        .filter { (_, distance) -> distance <= radiusMeters }
        .sortedBy { (_, distance) -> distance }
    return NearbyResponse(totalCount = inRange.size, items = inRange.take(limit).map { (complex, _) -> complex })
}

/** 서버의 앞쪽 단지 규칙: 반경 안에서 차가 향한 쪽 180도(좌우 90도) 안에 있는 단지 중 가장 가까운 하나 */
internal fun NearbyResponse.nearestAhead(
    lat: Double,
    lon: Double,
    heading: Double,
    radiusMeters: Int
): ComplexSummary? = items
    .map { complex -> complex to distanceMeters(lat, lon, complex.lat, complex.lon) }
    .filter { (complex, distance) ->
        distance <= radiusMeters && isAhead(bearingDegrees(lat, lon, complex.lat, complex.lon), heading)
    }
    .minByOrNull { (_, distance) -> distance }
    ?.first

/** [bearing] 쪽이 [heading]을 향한 차의 앞쪽 180도 안인지. 둘 다 북쪽 0도, 시계 방향이다 */
internal fun isAhead(bearing: Double, heading: Double): Boolean {
    val relative = ((bearing - heading) % FULL_TURN + FULL_TURN) % FULL_TURN
    return relative <= QUARTER_TURN || relative >= FULL_TURN - QUARTER_TURN
}

private const val FULL_TURN = 360.0
private const val QUARTER_TURN = 90.0

/** 서버의 geofences 규칙: ([lat], [lon]) 반경 안에서 알림 조건에 맞는 단지들 */
internal fun NearbyResponse.geofencesAround(
    lat: Double,
    lon: Double,
    radiusMeters: Int,
    conditions: AlertConditions
): List<ComplexSummary> = items.filter { complex ->
    distanceMeters(lat, lon, complex.lat, complex.lon) <= radiusMeters && conditions.matches(complex)
}

/** 서버의 저장 목록 규칙: 최근에 저장한 것부터 주고, 단지 요약은 주변 단지와 같은 모양에 saved만 true다. */
internal fun NearbyResponse.savedList(entries: List<FakeSavedEntry>): SavedResponse {
    val byId = items.associateBy { it.complexId }
    return SavedResponse(
        items = entries
            .sortedByDescending { it.savedAt }
            .mapNotNull { entry -> byId[entry.complexId]?.let { SavedComplex(entry.savedAt, it.copy(saved = true)) } }
    )
}

/**
 * 서버의 오늘 들은 단지 규칙: [since](오늘 0시)부터 들은 단지를 최근 것부터 주고,
 * 같은 단지는 마지막으로 들은 시각 하나만 둔다. 저장한 단지는 saved가 true다.
 */
internal fun NearbyResponse.heardList(
    entries: List<FakeHeardEntry>,
    since: OffsetDateTime,
    savedIds: Set<String>
): HeardResponse {
    val byId = items.associateBy { it.complexId }
    return HeardResponse(
        items = entries
            .filter { it.heardAt >= since }
            .sortedByDescending { it.heardAt }
            .distinctBy { it.complexId }
            .mapNotNull { entry ->
                byId[entry.complexId]?.let { HeardComplex(entry.heardAt, it.copy(saved = it.complexId in savedIds)) }
            }
    )
}
