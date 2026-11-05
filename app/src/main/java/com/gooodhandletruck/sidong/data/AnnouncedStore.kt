package com.gooodhandletruck.sidong.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.ZoneId

/**
 * 오늘 소리로 알려 준 단지. 같은 단지는 하루에 한 번만 자동으로 알린다(화면 설계서 '알림 규칙').
 * 주행 중에는 '오늘 들은 단지' 목록을 서버에서 받지 않으므로 이 기기에 따로 기억한다. 날짜가 바뀌면 비운다.
 */
class AnnouncedStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun wasAnnouncedToday(complexId: String): Boolean = complexId in todayIds()

    @Synchronized
    fun markAnnounced(complexId: String) {
        val ids = todayIds() + complexId
        prefs.edit {
            putString(KEY_DATE, today())
            putStringSet(KEY_IDS, ids)
        }
    }

    private fun todayIds(): Set<String> =
        if (prefs.getString(KEY_DATE, null) == today()) prefs.getStringSet(KEY_IDS, null).orEmpty() else emptySet()

    private fun today(): String = LocalDate.now(ZONE).toString()

    private companion object {
        const val PREFS_NAME = "announced"
        const val KEY_DATE = "date"
        const val KEY_IDS = "ids"
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
