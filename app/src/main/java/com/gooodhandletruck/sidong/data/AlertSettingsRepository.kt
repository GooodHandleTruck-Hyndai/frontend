package com.gooodhandletruck.sidong.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/** 알림 조건의 단지 유형. 오피스텔·원룸은 데이터가 준비될 때까지 고를 수 없다(화면 설계서 S-02) */
@Serializable
enum class ComplexType(val available: Boolean) {
    APT(available = true),
    OFFICETEL(available = false),
    ONE_ROOM(available = false);

    companion object {
        /** 서버의 type 값. 오피스텔·원룸 값은 아직 정해지지 않았다 */
        fun of(type: String): ComplexType? = if (type == "APT") APT else null
    }
}

/** 알림 조건의 가격대(만원). [minManwon]보다 크고 [maxManwon] 이하다. null이면 그쪽 끝이 없다 */
@Serializable
enum class PriceRange(val minManwon: Int?, val maxManwon: Int?) {
    UP_TO_5_EOK(null, 50_000),
    FROM_5_TO_10_EOK(50_000, 100_000),
    FROM_10_TO_15_EOK(100_000, 150_000),
    OVER_15_EOK(150_000, null);

    fun contains(priceManwon: Int): Boolean =
        (minManwon == null || priceManwon > minManwon) && (maxManwon == null || priceManwon <= maxManwon)
}

/** 알림 조건의 전용면적(㎡). [minM2]보다 크고 [maxM2] 이하다. null이면 그쪽 끝이 없다 */
@Serializable
enum class AreaRange(val minM2: Int?, val maxM2: Int?) {
    UP_TO_60(null, 60),
    FROM_60_TO_85(60, 85),
    OVER_85(85, null);

    fun contains(areaM2: Double): Boolean = (minM2 == null || areaM2 > minM2) && (maxM2 == null || areaM2 <= maxM2)
}

/** S-02에서 고른 알림 조건. 항목마다 여러 개를 고를 수 있고, 처음에는 고를 수 있는 것을 모두 고른 상태다 */
@Serializable
data class AlertConditions(
    val types: Set<ComplexType> = ComplexType.entries.filter { it.available }.toSet(),
    val prices: Set<PriceRange> = PriceRange.entries.toSet(),
    val areas: Set<AreaRange> = AreaRange.entries.toSet()
) {
    /** 항목마다 하나 이상 골랐는지. 하나라도 비어 있으면 맞는 단지가 없어서 저장할 수 없다 */
    val isComplete: Boolean
        get() = types.isNotEmpty() && prices.isNotEmpty() && areas.isNotEmpty()

    /**
     * 단지가 조건에 맞는지(주행 중 자동 알림). 가격·면적은 카드의 대표 평형으로 본다.
     * 최근 3개월 평균이 없으면 마지막 거래 가격으로 보고, 거래 기록이 아예 없으면 가격대·면적을 모두 골랐을 때만 맞는다.
     */
    fun matches(complex: ComplexSummary): Boolean {
        val type = ComplexType.of(complex.type) ?: return false
        if (type !in types) return false
        val allPrices = prices.size == PriceRange.entries.size
        val price = complex.price ?: return allPrices && areas.size == AreaRange.entries.size
        val priceManwon = price.avgPriceManwon ?: price.lastTrade?.priceManwon
        val priceOk = allPrices || (priceManwon != null && prices.any { it.contains(priceManwon) })
        return priceOk && areas.any { it.contains(price.areaM2) }
    }
}

/** 주행 중 자동 알림 설정. [conditions]가 null이면 아직 조건을 정하지 않았다 */
data class AlertSettings(
    val enabled: Boolean = false,
    val conditions: AlertConditions? = null
) {
    /** 조건을 정하고 알림을 켰는지. 그때만 주행 중에 단지를 지나면 말한다 */
    val isOn: Boolean
        get() = enabled && conditions != null
}

/**
 * 알림 조건과 켜기·끄기를 이 기기(SharedPreferences)에 기억한다.
 * TODO: API 초안의 '관심 조건 조회·저장'(P1) 형식이 정해지면 서버와 맞춘다. 지금은 이 기기에만 있다
 */
class AlertSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AlertSettings> = _settings.asStateFlow()

    /** S-02 [이 조건으로 알림 받기]·[저장]: 조건을 저장하고 알림을 켠다 */
    fun saveConditions(conditions: AlertConditions) {
        _settings.update { AlertSettings(enabled = true, conditions = conditions) }
        prefs.edit {
            putString(KEY_CONDITIONS, AppJson.encodeToString(AlertConditions.serializer(), conditions))
            putBoolean(KEY_ENABLED, true)
        }
    }

    /** S-02 [나중에 할게요]는 끄고, S-07 스위치는 켜거나 끈다. 조건은 그대로 둔다 */
    fun setEnabled(enabled: Boolean) {
        _settings.update { it.copy(enabled = enabled) }
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
    }

    private fun load(): AlertSettings {
        val conditions = prefs.getString(KEY_CONDITIONS, null)?.let { json ->
            try {
                AppJson.decodeFromString(AlertConditions.serializer(), json)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "기억한 알림 조건을 읽지 못해 비웠어요", e)
                null
            }
        }
        return AlertSettings(enabled = prefs.getBoolean(KEY_ENABLED, false), conditions = conditions)
    }

    private companion object {
        const val TAG = "AlertSettings"
        const val PREFS_NAME = "alert_settings"
        const val KEY_CONDITIONS = "conditions"
        const val KEY_ENABLED = "enabled"
    }
}
