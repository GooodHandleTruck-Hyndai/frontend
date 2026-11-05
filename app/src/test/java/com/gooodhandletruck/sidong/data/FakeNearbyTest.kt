package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.NearbyResponse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 목업 파일이 데이터 클래스와 맞는지, 주변 단지 규칙(반경·가까운 순·개수)이 맞는지 본다. */
class FakeNearbyTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val mock: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())

    private val lat = MockCenter.LAT
    private val lon = MockCenter.LON

    @Test
    fun mockFileParses() = assertEquals(18, mock.items.size)

    @Test
    fun oneKilometerCountsTwelveAndReturnsTen() {
        val result = mock.within(lat, lon, radiusMeters = 1000, limit = 10)
        assertEquals(12, result.totalCount)
        assertEquals(10, result.items.size)
    }

    @Test
    fun otherRadiusCounts() {
        assertEquals(4, mock.within(lat, lon, radiusMeters = 500, limit = 10).totalCount)
        assertEquals(18, mock.within(lat, lon, radiusMeters = 3000, limit = 10).totalCount)
    }

    @Test
    fun sortedByDistance() {
        val distances = mock.within(lat, lon, radiusMeters = 3000, limit = 18).items
            .map { distanceMeters(lat, lon, it.lat, it.lon) }
        assertEquals(distances.sorted(), distances)
    }

    @Test
    fun missingPricesAreNull() {
        val byId = mock.items.associateBy { it.complexId }
        // 한솔 5단지: 최근 3개월 거래가 없어 평균이 없다
        assertNull(byId.getValue("c-1002").price?.avgPriceManwon)
        // 바람꽃 1단지: 거래 기록이 아예 없다
        assertNull(byId.getValue("c-1017").price)
    }
}
