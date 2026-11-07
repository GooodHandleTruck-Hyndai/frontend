package com.gooodhandletruck.zippick.data

import com.gooodhandletruck.zippick.data.model.NearbyResponse
import com.gooodhandletruck.zippick.data.model.SavedResponse
import java.io.File
import java.time.OffsetDateTime
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 저장 목록 규칙(최근에 저장한 것부터, saved는 true)과 시각 형식(+09:00, 초까지)을 본다. */
class FakeSavedTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val nearby: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())

    private fun entry(complexId: String, time: String) = FakeSavedEntry(complexId, OffsetDateTime.parse(time))

    @Test
    fun newestSavedComesFirst() {
        val list = nearby.savedList(
            listOf(
                entry("c-1003", "2026-11-05T09:00:00+09:00"),
                entry("c-1001", "2026-11-05T10:30:00+09:00"),
                entry("c-1002", "2026-11-04T21:00:00+09:00")
            )
        )
        assertEquals(listOf("c-1001", "c-1003", "c-1002"), list.items.map { it.complex.complexId })
        assertTrue(list.items.all { it.complex.saved })
    }

    @Test
    fun unknownComplexIsDropped() {
        val list = nearby.savedList(listOf(entry("c-9999", "2026-11-05T09:00:00+09:00")))
        assertTrue(list.items.isEmpty())
    }

    @Test
    fun savedAtKeepsSecondsAndOffset() {
        val json = AppJson.encodeToString(listOf(entry("c-1001", "2026-11-05T09:12:00+09:00")))
        assertTrue(json, json.contains("\"2026-11-05T09:12:00+09:00\""))
    }

    @Test
    fun apiExampleParses() {
        // API 제안 6번 응답 예시의 모양
        val response = AppJson.decodeFromString<SavedResponse>(
            """{"items":[{"savedAt":"2026-11-05T09:12:00+09:00","complex":${complexJson()}}]}"""
        )
        assertEquals(OffsetDateTime.parse("2026-11-05T09:12:00+09:00"), response.items.single().savedAt)
    }

    private fun complexJson() =
        """{"complexId":"c-1001","type":"APT","name":"한빛마을 3단지","address":"서울특별시 강남구 예시로 12",""" +
            """"lat":37.5003,"lon":127.0311,"imageUrl":null,"saved":true,"price":null}"""
}
