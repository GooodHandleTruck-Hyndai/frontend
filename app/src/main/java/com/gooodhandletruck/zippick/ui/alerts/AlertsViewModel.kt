package com.gooodhandletruck.zippick.ui.alerts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.zippick.ZippickApp
import com.gooodhandletruck.zippick.data.remote.NewTradeAlert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S-10 새 거래 알림 화면 상태 */
sealed interface AlertsContent {
    data object Loading : AlertsContent

    /** [alerts]는 최신순이다. 비어 있으면 안내 상자를 보여 준다 */
    data class Loaded(val alerts: List<NewTradeAlert>) : AlertsContent

    data object Failed : AlertsContent
}

/** S-10을 맡는다. 들어올 때 최근 7일 알림을 받고, 알림을 누르면 읽음으로 바꾼 뒤 단지 상세를 연다 */
class AlertsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as ZippickApp).container.alertInboxRepository

    private val _content = MutableStateFlow<AlertsContent>(AlertsContent.Loading)
    val content: StateFlow<AlertsContent> = _content.asStateFlow()

    private var loadJob: Job? = null

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _content.value = AlertsContent.Loaded(repository.alerts())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "새 거래 알림을 불러오지 못했어요", e)
                if (_content.value !is AlertsContent.Loaded) _content.value = AlertsContent.Failed
            }
        }
    }

    fun retry() {
        _content.value = AlertsContent.Loading
        refresh()
    }

    /** 화면에서는 바로 읽음으로 보이고, 서버에는 뒤에서 보낸다. 실패하면 다음에 열 때 다시 안 읽음으로 온다 */
    fun markRead(alert: NewTradeAlert) {
        if (alert.read) return
        _content.update { content ->
            if (content is AlertsContent.Loaded) {
                content.copy(
                    alerts = content.alerts.map { if (it.alertId == alert.alertId) it.copy(read = true) else it }
                )
            } else {
                content
            }
        }
        viewModelScope.launch {
            try {
                repository.markRead(alert.alertId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "알림 ${alert.alertId} 읽음을 보내지 못했어요", e)
            }
        }
    }

    private companion object {
        const val TAG = "Alerts"
    }
}
