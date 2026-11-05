package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.NearbyResponse
import java.io.File
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 오늘 들은 단지 규칙: 오늘 0시부터, 최근 것부터, 같은 단지는 마지막으로 들은 시각 하나만. */
class FakeHeardTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val nearby: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())

    private val today = OffsetDateTime.parse("2026-11-05T00:00:00+09:00")

    private fun heard(complexId: String, time: String) = FakeHeardEntry(complexId, OffsetDateTime.parse(time))

    @Test
    fun onlyTodayNewestFirstAndOncePerComplex() {
        val list = nearby.heardList(
            listOf(
                heard("c-1003", "2026-11-05T14:32:00+09:00"),
                heard("c-1001", "2026-11-05T09:10:00+09:00"),
                heard("c-1003", "2026-11-05T08:00:00+09:00"),
                heard("c-1005", "2026-11-04T23:59:00+09:00")
            ),
            since = today,
            savedIds = emptySet()
        )
        assertEquals(listOf("c-1003", "c-1001"), list.items.map { it.complex.complexId })
        assertEquals(OffsetDateTime.parse("2026-11-05T14:32:00+09:00"), list.items.first().heardAt)
    }

    @Test
    fun savedComplexIsMarked() {
        val list = nearby.heardList(
            listOf(heard("c-1001", "2026-11-05T09:10:00+09:00"), heard("c-1003", "2026-11-05T10:00:00+09:00")),
            since = today,
            savedIds = setOf("c-1001")
        )
        val byId = list.items.associateBy { it.complex.complexId }
        assertTrue(byId.getValue("c-1001").complex.saved)
        assertFalse(byId.getValue("c-1003").complex.saved)
    }
}
