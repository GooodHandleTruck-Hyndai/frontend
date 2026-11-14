package com.gooodhandletruck.zippick.ui.history

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.zippick.ZippickApp
import com.gooodhandletruck.zippick.data.CarLocation
import com.gooodhandletruck.zippick.data.LocationState
import com.gooodhandletruck.zippick.data.model.HeardComplex
import com.gooodhandletruck.zippick.data.model.hasCoordinate
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel
import com.gooodhandletruck.zippick.ui.component.placeOf
import com.gooodhandletruck.zippick.ui.format.formatClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S-06 오늘 들은 단지의 상태를 맡는다. 주행 중에 말한 단지(HEARD)를 최근 것부터 보여 주고,
 * 차 위치는 카드의 방향·거리 칩에만 쓴다. 위치가 바뀌어도 서버를 다시 부르지 않는다.
 */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ZippickApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var items: List<HeardComplex>? = null
    private var car: CarLocation? = null
    private var loadJob: Job? = null

    /** 화면에 들어올 때와 1:2 창에서 상세를 닫을 때 부른다. 보던 목록이 있으면 둔 채 새로 받아 온다 */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                items = repository.heardToday().items
                showRows()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "오늘 들은 단지를 불러오지 못했어요", e)
                if (items == null) _state.update { it.copy(content = HistoryContent.Failed) }
            }
        }
    }

    /** [다시 시도] */
    fun retry() {
        _state.update { it.copy(content = HistoryContent.Loading) }
        refresh()
    }

    /** S-06이 화면에 보이는 동안 부른다. 차가 움직이면 방향·거리 칩만 다시 계산한다 */
    suspend fun followLocation() {
        locationRepository.state.collect { location ->
            if (location is LocationState.Found) {
                car = location.location
                showRows()
            }
        }
    }

    private fun showRows() {
        val items = items ?: return
        val car = car
        val rows = items.map { heard ->
            val complex = heard.complex
            HeardRow(
                time = formatClock(heard.heardAt),
                card = ComplexCardModel(
                    complex = complex,
                    place = car?.takeIf { complex.hasCoordinate }?.placeOf(complex.lat, complex.lon)
                )
            )
        }
        _state.update { it.copy(content = HistoryContent.Loaded(rows)) }
    }

    private companion object {
        const val TAG = "History"
    }
}
