package com.gooodhandletruck.sidong.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gooodhandletruck.sidong.SidongApp
import com.gooodhandletruck.sidong.data.AlertConditions
import com.gooodhandletruck.sidong.data.AreaRange
import com.gooodhandletruck.sidong.data.ComplexType
import com.gooodhandletruck.sidong.data.PriceRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * S-02 알림 조건. 고르는 동안은 [draft]에만 두고, [이 조건으로 알림 받기]·[저장]을 눌러야 저장한다.
 * 화면을 열 때마다 [startEditing]으로 저장한 조건(없으면 모두 고른 상태)에서 시작한다.
 */
class AlertSetupViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as SidongApp).container.alertSettingsRepository

    private val _draft = MutableStateFlow(AlertConditions())
    val draft: StateFlow<AlertConditions> = _draft.asStateFlow()

    fun startEditing() {
        _draft.value = store.settings.value.conditions ?: AlertConditions()
    }

    fun toggleType(type: ComplexType) = _draft.update { it.copy(types = it.types.toggled(type)) }

    fun togglePrice(range: PriceRange) = _draft.update { it.copy(prices = it.prices.toggled(range)) }

    fun toggleArea(range: AreaRange) = _draft.update { it.copy(areas = it.areas.toggled(range)) }

    /** [이 조건으로 알림 받기]·[저장]: 조건을 저장하고 주행 중 알림을 켠다 */
    fun accept() {
        val conditions = _draft.value
        if (conditions.isComplete) store.saveConditions(conditions)
    }

    /** [나중에 할게요]: 주행 중 자동 알림을 끈 채로 둔다(기획.md 결정) */
    fun later() = store.setEnabled(false)

    private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value
}
