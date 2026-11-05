package com.gooodhandletruck.sidong.ui.nearby

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.SidongApp
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.LocationState
import com.gooodhandletruck.sidong.data.movedFrom
import com.gooodhandletruck.sidong.ui.component.toCardModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S-03 주변 단지의 상태를 맡는다. 화면이 보이는 동안 차 위치를 따라가며, 움직이면 목록을 다시 불러온다.
 * Activity가 살아 있는 동안 남아 있어서, 다른 탭에 갔다 와도 고른 범위와 목록이 그대로다.
 */
class NearbyViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SidongApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository
    private val rangeStore = container.nearbyRangeStore

    private val _state = MutableStateFlow(NearbyUiState())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    /** 지금 목록을 계산한 위치. 여기서 50m 넘게 움직이거나 30도 넘게 돌면 다시 불러온다 */
    private var anchor: CarLocation? = null

    private var loadJob: Job? = null

    init {
        // S-07 '주변 범위 기본값'이 처음 범위이고, 설정에서 바꾸면 S-03 범위도 바로 바뀐다
        viewModelScope.launch {
            rangeStore.defaultRadiusMeters.collect { meters ->
                NearbyRadius.entries.firstOrNull { it.meters == meters }?.let(::selectRadius)
            }
        }
    }

    /**
     * S-03이 화면에 보이는 동안 부른다. 부른 쪽이 취소할 때까지 위치를 따라간다.
     * 주행 중에는 S-08이 S-03을 덮어 취소되므로 단지 목록을 부르지 않는다(주행 중 서버 호출 규칙).
     * 정차해서 S-03이 다시 보이면 그사이 움직인 위치로 바로 다시 불러온다(설계 원칙 '정차하면 탭 없이 답한다').
     */
    suspend fun followLocation() {
        locationRepository.state.collect(::onLocationChanged)
    }

    fun selectRadius(radius: NearbyRadius) {
        if (radius == _state.value.radius) return
        _state.update { it.copy(radius = radius) }
        // 위치를 아직 못 받았으면 위치가 오면 고른 범위로 부른다
        anchor?.let(::load)
    }

    /** [다시 시도]. 위치를 못 받았으면 위치를, 가격을 못 받았으면 목록을 다시 부른다 */
    fun retry() {
        val location = anchor
        if (location == null) locationRepository.retry() else load(location)
    }

    private fun onLocationChanged(location: LocationState) {
        when (location) {
            // 이미 목록이 있으면 위치를 다시 찾는 동안에도 그대로 둔다
            LocationState.Searching -> if (anchor == null) showContent(NearbyContent.Loading)
            LocationState.Unavailable -> if (anchor == null) showContent(NearbyContent.LocationFailed)
            is LocationState.Found -> {
                val last = anchor
                if (last == null || location.location.movedFrom(last)) load(location.location)
            }
        }
    }

    private fun load(location: CarLocation) {
        anchor = location
        loadJob?.cancel()
        val radius = _state.value.radius
        showContent(NearbyContent.Loading)
        loadJob = viewModelScope.launch {
            val content = try {
                val response = repository.nearby(location.lat, location.lon, radius.meters)
                NearbyContent.Loaded(
                    totalCount = response.totalCount,
                    cards = response.items.map { it.toCardModel(location) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "주변 단지를 불러오지 못했어요", e)
                NearbyContent.Failed
            }
            showContent(content)
        }
    }

    private fun showContent(content: NearbyContent) = _state.update { it.copy(content = content) }

    private companion object {
        const val TAG = "Nearby"
    }
}
