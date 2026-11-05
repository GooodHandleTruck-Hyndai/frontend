package com.gooodhandletruck.sidong.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** S-07 '주변 범위 기본값'(500m·1km·3km)을 이 기기에 기억한다. S-03 범위 칩의 처음 값이다 */
class NearbyRangeStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _defaultRadiusMeters = MutableStateFlow(prefs.getInt(KEY_RADIUS, DEFAULT_RADIUS_METERS))
    val defaultRadiusMeters: StateFlow<Int> = _defaultRadiusMeters.asStateFlow()

    fun setDefaultRadius(meters: Int) {
        _defaultRadiusMeters.value = meters
        prefs.edit { putInt(KEY_RADIUS, meters) }
    }

    private companion object {
        const val PREFS_NAME = "nearby"
        const val KEY_RADIUS = "default_radius_m"
        const val DEFAULT_RADIUS_METERS = 1000 // 화면 설계서 S-03: 기본은 1km
    }
}
