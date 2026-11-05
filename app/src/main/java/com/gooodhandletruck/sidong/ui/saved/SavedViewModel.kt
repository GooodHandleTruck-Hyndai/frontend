package com.gooodhandletruck.sidong.ui.saved

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.SidongApp
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.LocationState
import com.gooodhandletruck.sidong.data.model.SavedComplex
import com.gooodhandletruck.sidong.ui.component.ComplexCardModel
import com.gooodhandletruck.sidong.ui.component.placeOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S-05 저장한 단지의 상태를 맡는다. 목록은 저장한 순(서버 순서)이고, 차 위치는 카드의 방향·거리 칩에만 쓴다.
 * 위치가 바뀌어도 서버를 다시 부르지 않는다.
 */
class SavedViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SidongApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository

    private val _state = MutableStateFlow(SavedUiState())
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    /** 마지막으로 받은 저장 목록과 차 위치. 둘 중 하나가 바뀌면 카드를 다시 만든다 */
    private var items: List<SavedComplex>? = null
    private var car: CarLocation? = null

    private var loadJob: Job? = null

    /**
     * 화면에 들어올 때와 1:2 창에서 상세를 닫을 때 부른다. 보던 목록이 있으면 둔 채 새로 받아 온다.
     * 상세에서 저장을 취소한 단지는 이때 목록에서 빠진다.
     */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                items = repository.saved().items
                showCards()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "저장한 단지를 불러오지 못했어요", e)
                // 보던 목록이 있으면 그대로 두고, 없을 때만 오류 상자를 띄운다
                if (items == null) _state.update { it.copy(content = SavedContent.Failed) }
            }
        }
    }

    /** [다시 시도] */
    fun retry() {
        _state.update { it.copy(content = SavedContent.Loading) }
        refresh()
    }

    /** S-05가 화면에 보이는 동안 부른다. 차가 움직이면 방향·거리 칩만 다시 계산한다 */
    suspend fun followLocation() {
        locationRepository.state.collect { location ->
            if (location is LocationState.Found) {
                car = location.location
                showCards()
            }
        }
    }

    private fun showCards() {
        val items = items ?: return
        val car = car
        val cards = items.map { saved ->
            ComplexCardModel(complex = saved.complex, place = car?.placeOf(saved.complex.lat, saved.complex.lon))
        }
        _state.update { it.copy(content = SavedContent.Loaded(cards)) }
    }

    private companion object {
        const val TAG = "Saved"
    }
}
