package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.NearbyResponse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 근접 알림을 걸 단지: 반경 안에서 알림 조건에 맞는 단지만. */
class FakeGeofenceTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val mock: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())

    @Test
    fun allConditionsTakeEveryComplexInRange() {
        val all = mock.geofencesAround(
            MockCenter.LAT,
            MockCenter.LON,
            radiusMeters = 3000,
            conditions = AlertConditions()
        )
        assertEquals(18, all.size)
    }

    @Test
    fun conditionsNarrowTheTargets() {
        val upTo60 = AlertConditions(areas = setOf(AreaRange.UP_TO_60))
        val targets = mock.geofencesAround(MockCenter.LAT, MockCenter.LON, radiusMeters = 3000, conditions = upTo60)
        assertTrue(targets.isNotEmpty())
        assertTrue(targets.all { (it.price?.areaM2 ?: 0.0) <= 60.0 })
    }
}
