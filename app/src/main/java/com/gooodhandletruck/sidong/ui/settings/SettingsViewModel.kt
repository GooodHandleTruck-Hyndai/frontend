package com.gooodhandletruck.sidong.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gooodhandletruck.sidong.ZippickApp
import com.gooodhandletruck.sidong.data.AlertSettings
import kotlinx.coroutines.flow.StateFlow

/** S-07 설정. 주행 중 음성 알림 켜기·끄기와 주변 범위 기본값을 바로 저장한다 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ZippickApp).container
    private val alertStore = container.alertSettingsRepository
    private val rangeStore = container.nearbyRangeStore

    val alertSettings: StateFlow<AlertSettings> = alertStore.settings
    val defaultRadiusMeters: StateFlow<Int> = rangeStore.defaultRadiusMeters

    /**
     * '주행 중 음성 알림' 스위치. 조건을 아직 정하지 않았는데 켜면 저장하지 않고 false를 돌려준다.
     * 그때 화면은 S-02를 열고, S-02에서 [저장]하면 알림이 켜진다(화면 설계서 S-07).
     */
    fun setAlertsEnabled(enabled: Boolean): Boolean {
        if (enabled && alertSettings.value.conditions == null) return false
        alertStore.setEnabled(enabled)
        return true
    }

    fun setDefaultRadius(meters: Int) = rangeStore.setDefaultRadius(meters)
}
