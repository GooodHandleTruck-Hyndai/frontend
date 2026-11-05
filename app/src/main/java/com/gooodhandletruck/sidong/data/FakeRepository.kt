package com.gooodhandletruck.sidong.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.data.model.OffsetDateTimeSerializer
import com.gooodhandletruck.sidong.data.model.SavedComplex
import com.gooodhandletruck.sidong.data.model.SavedResponse
import java.time.DateTimeException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * 서버 전에 쓰는 단지 저장소. app/src/main/assets/mock/의 JSON을 읽는다. 단지 이름·주소·가격은 가상 데이터다.
 * - 주변 단지: 시연 지역(강남역 둘레)의 단지를 한 파일에 모두 넣어 두고, 요청한 위치·반경으로 걸러 서버처럼 답한다.
 * - 단지 상세: 상세 응답을 단지마다 하나씩 배열로 넣어 두고 complexId로 찾는다.
 * - 저장: 서버 대신 이 기기(SharedPreferences)에 기억해서, 앱을 다시 켜도 남는다. 앱 데이터를 지우면 사라진다.
 */
class FakeRepository(context: Context) : ComplexRepository {

    private val assets = context.applicationContext.assets
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 처음 부를 때 한 번만 읽는다
    private val allNearby: NearbyResponse by lazy { read(NEARBY_FILE) }
    private val details: Map<String, ComplexDetail> by lazy {
        read<List<ComplexDetail>>(DETAILS_FILE).associateBy { it.complexId }
    }

    private val savedLock = Any()

    // savedLock 안에서만 읽고 쓴다. 처음 쓸 때 기기에서 읽어 온다
    private var savedCache: List<FakeSavedEntry>? = null

    override suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse =
        withContext(Dispatchers.IO) {
            delay(FAKE_LATENCY_MS) // 실제 통신처럼 '불러오는 중' 화면이 잠깐 보이게 한다
            val response = allNearby.within(lat, lon, radiusMeters, limit)
            val saved = savedIds()
            response.copy(items = response.items.map { it.copy(saved = it.complexId in saved) })
        }

    override suspend fun detail(complexId: String): ComplexDetail = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        val detail = details[complexId] ?: throw ComplexNotFoundException(complexId)
        detail.copy(saved = complexId in savedIds())
    }

    override suspend fun setSaved(complexId: String, saved: Boolean) = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        synchronized(savedLock) {
            val entries = savedEntries()
            val updated = when {
                // 이미 저장한 단지를 다시 저장해도 저장한 시각은 그대로다(PUT은 두 번 보내도 결과가 같다)
                saved && entries.any { it.complexId == complexId } -> entries
                saved -> listOf(FakeSavedEntry(complexId, now())) + entries
                else -> entries.filterNot { it.complexId == complexId }
            }
            if (updated != entries) {
                savedCache = updated
                prefs.edit { putString(KEY_SAVED, AppJson.encodeToString(updated)) }
            }
        }
    }

    override suspend fun saved(): SavedResponse = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        allNearby.savedList(synchronized(savedLock) { savedEntries() })
    }

    private fun savedIds(): Set<String> = synchronized(savedLock) { savedEntries().map { it.complexId }.toSet() }

    // savedLock 안에서 부른다
    private fun savedEntries(): List<FakeSavedEntry> = savedCache ?: loadSaved().also { savedCache = it }

    private fun loadSaved(): List<FakeSavedEntry> {
        val json = prefs.getString(KEY_SAVED, null) ?: return emptyList()
        return try {
            AppJson.decodeFromString(json)
        } catch (e: IllegalArgumentException) { // JSON이 깨졌을 때(SerializationException 포함)
            Log.w(TAG, "기기에 기억한 저장 목록을 읽지 못해 비웠어요", e)
            emptyList()
        } catch (e: DateTimeException) {
            Log.w(TAG, "기기에 기억한 저장 시각을 읽지 못해 비웠어요", e)
            emptyList()
        }
    }

    private fun now(): OffsetDateTime = OffsetDateTime.now(ZoneId.of(SERVER_ZONE)).truncatedTo(ChronoUnit.SECONDS)

    private inline fun <reified T> read(path: String): T =
        assets.open(path).bufferedReader().use { AppJson.decodeFromString(it.readText()) }

    private companion object {
        const val TAG = "FakeRepository"
        const val NEARBY_FILE = "mock/complexes_nearby.json"
        const val DETAILS_FILE = "mock/complex_details.json"
        const val FAKE_LATENCY_MS = 400L
        const val PREFS_NAME = "fake_server"
        const val KEY_SAVED = "saved"
        const val SERVER_ZONE = "Asia/Seoul" // 시각은 +09:00으로 주고받는다
    }
}

/** 목업 서버가 기억하는 저장 한 건 */
@Serializable
internal data class FakeSavedEntry(
    val complexId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val savedAt: OffsetDateTime
)

/** 서버의 주변 단지 규칙: (lat, lon)에서 반경 안의 단지를 가까운 순으로 limit개 주고, totalCount는 반경 안 전체 수다. */
internal fun NearbyResponse.within(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse {
    val inRange = items
        .map { complex -> complex to distanceMeters(lat, lon, complex.lat, complex.lon) }
        .filter { (_, distance) -> distance <= radiusMeters }
        .sortedBy { (_, distance) -> distance }
    return NearbyResponse(totalCount = inRange.size, items = inRange.take(limit).map { (complex, _) -> complex })
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
