package com.gooodhandletruck.sidong.data

import android.content.Context
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * 서버 전에 쓰는 단지 저장소. app/src/main/assets/mock/의 JSON을 읽는다. 단지 이름·주소·가격은 가상 데이터다.
 * - 주변 단지: 시연 지역(강남역 둘레)의 단지를 한 파일에 모두 넣어 두고, 요청한 위치·반경으로 걸러 서버처럼 답한다.
 * - 단지 상세: 상세 응답을 단지마다 하나씩 배열로 넣어 두고 complexId로 찾는다.
 * - 저장: 서버 대신 앱이 살아 있는 동안만 기억한다. 앱을 다시 켜면 모두 저장하지 않은 상태다.
 */
class FakeRepository(context: Context) : ComplexRepository {

    private val assets = context.applicationContext.assets

    // 처음 부를 때 한 번만 읽는다
    private val allNearby: NearbyResponse by lazy { read(NEARBY_FILE) }
    private val details: Map<String, ComplexDetail> by lazy {
        read<List<ComplexDetail>>(DETAILS_FILE).associateBy { it.complexId }
    }

    private val savedIds = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun nearby(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse =
        withContext(Dispatchers.IO) {
            delay(FAKE_LATENCY_MS) // 실제 통신처럼 '불러오는 중' 화면이 잠깐 보이게 한다
            val response = allNearby.within(lat, lon, radiusMeters, limit)
            val saved = savedIds.value
            response.copy(items = response.items.map { it.copy(saved = it.complexId in saved) })
        }

    override suspend fun detail(complexId: String): ComplexDetail = withContext(Dispatchers.IO) {
        delay(FAKE_LATENCY_MS)
        val detail = details[complexId] ?: throw ComplexNotFoundException(complexId)
        detail.copy(saved = complexId in savedIds.value)
    }

    override suspend fun setSaved(complexId: String, saved: Boolean) {
        delay(FAKE_LATENCY_MS)
        savedIds.update { if (saved) it + complexId else it - complexId }
    }

    private inline fun <reified T> read(path: String): T =
        assets.open(path).bufferedReader().use { AppJson.decodeFromString(it.readText()) }

    private companion object {
        const val NEARBY_FILE = "mock/complexes_nearby.json"
        const val DETAILS_FILE = "mock/complex_details.json"
        const val FAKE_LATENCY_MS = 400L
    }
}

/** 서버의 주변 단지 규칙: (lat, lon)에서 반경 안의 단지를 가까운 순으로 limit개 주고, totalCount는 반경 안 전체 수다. */
internal fun NearbyResponse.within(lat: Double, lon: Double, radiusMeters: Int, limit: Int): NearbyResponse {
    val inRange = items
        .map { complex -> complex to distanceMeters(lat, lon, complex.lat, complex.lon) }
        .filter { (_, distance) -> distance <= radiusMeters }
        .sortedBy { (_, distance) -> distance }
    return NearbyResponse(totalCount = inRange.size, items = inRange.take(limit).map { (complex, _) -> complex })
}
