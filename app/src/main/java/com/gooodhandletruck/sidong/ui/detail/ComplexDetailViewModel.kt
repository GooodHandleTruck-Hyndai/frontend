package com.gooodhandletruck.sidong.ui.detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.SidongApp
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.ComplexNotFoundException
import com.gooodhandletruck.sidong.data.LocationState
import com.gooodhandletruck.sidong.data.model.defaultArea
import com.gooodhandletruck.sidong.ui.component.placeOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S-04 단지 상세의 상태를 맡는다. 상세는 한 번에 하나만 열리므로(Navigator) ViewModel 하나가 지금 단지를 맡는다.
 * 저장은 누르는 즉시 화면에 반영하고 서버에 보낸다(화면 설계서 S-04 '버튼 동작').
 */
class ComplexDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SidongApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository

    private val _state = MutableStateFlow(ComplexDetailUiState())
    val state: StateFlow<ComplexDetailUiState> = _state.asStateFlow()

    /** 마지막으로 받은 차 위치. 단지를 바꿔 열어도 방향·거리 칩을 바로 계산한다 */
    private var car: CarLocation? = null

    private var loadJob: Job? = null

    /** 단지마다 서버에 보내는 중인 저장. 다른 단지로 바꿔 열어도 보내던 저장은 끝까지 보낸다 */
    private val saveJobs = mutableMapOf<String, Job>()

    /**
     * 화면에 단지를 띄울 때마다 부른다. 다른 단지면 처음부터 불러오고, 같은 단지면 보던 내용을 둔 채 새로 받아 온다.
     * 다른 화면(S-05 저장 목록, S-09 주행 중 저장)에서 저장 여부가 바뀌었을 수 있어서다.
     */
    fun open(complexId: String) {
        if (_state.value.complexId != complexId) _state.value = ComplexDetailUiState(complexId = complexId)
        load(complexId)
    }

    /** [다시 시도] */
    fun retry() {
        val complexId = _state.value.complexId ?: return
        _state.update { it.copy(content = DetailContent.Loading) }
        load(complexId)
    }

    fun selectArea(areaM2: Double) = _state.update { it.copy(selectedAreaM2 = areaM2) }

    /** [저장]·[저장했어요]. 화면을 먼저 바꾸고 서버에 보낸다. 서버가 실패하면 되돌린다 */
    fun toggleSaved() {
        val now = _state.value
        val complexId = now.complexId ?: return
        if (now.content !is DetailContent.Loaded) return
        val saved = !now.saved
        _state.update { it.copy(saved = saved) }
        // 빨리 여러 번 눌러도 마지막으로 누른 상태만 보낸다. 같은 값을 다시 보내도 결과가 같다(API 제안 4·5번)
        saveJobs[complexId]?.cancel()
        saveJobs[complexId] = viewModelScope.launch {
            try {
                repository.setSaved(complexId, saved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "저장을 바꾸지 못했어요", e)
                // TODO: 통신이 끊기면 다시 보낸다(API 검토 '고칠 것' 7번). 실패를 알릴 문구는 화면 설계서에 아직 없다
                _state.update { if (it.complexId == complexId) it.copy(saved = !saved) else it }
            }
        }
    }

    /**
     * S-04가 화면에 보이는 동안 부른다. 차가 움직이거나 돌면 방향·거리 칩을 다시 계산한다.
     * 서버는 부르지 않는다. 위치를 못 받으면 마지막 칩을 그대로 둔다.
     */
    suspend fun followLocation() {
        locationRepository.state.collect { location ->
            if (location is LocationState.Found) {
                car = location.location
                _state.update { it.withPlace() }
            }
        }
    }

    private fun load(complexId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val content = try {
                DetailContent.Loaded(repository.detail(complexId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: ComplexNotFoundException) {
                Log.w(TAG, "없는 단지예요: $complexId", e)
                DetailContent.NotFound
            } catch (e: Exception) {
                Log.w(TAG, "단지 상세를 불러오지 못했어요", e)
                DetailContent.Failed
            }
            _state.update { current ->
                when {
                    current.complexId != complexId -> current
                    content is DetailContent.Loaded -> current.showing(content)
                    // 보던 내용을 새로 받다가 실패하면 보던 내용을 그대로 둔다
                    current.content is DetailContent.Loaded -> current
                    else -> current.copy(content = content)
                }
            }
        }
    }

    private fun ComplexDetailUiState.showing(loaded: DetailContent.Loaded): ComplexDetailUiState {
        val areas = loaded.detail.areas
        // 같은 단지를 새로 받았으면 고른 평형을 그대로 두고, 처음이면 대표 평형을 고른다
        val area = selectedAreaM2?.takeIf { selected -> areas.any { it.areaM2 == selected } }
            ?: loaded.detail.defaultArea?.areaM2
        // 저장을 보내는 중이면 서버 값보다 방금 누른 값이 맞다
        val sending = saveJobs[loaded.detail.complexId]?.isActive == true
        return copy(
            content = loaded,
            selectedAreaM2 = area,
            saved = if (sending) saved else loaded.detail.saved
        ).withPlace()
    }

    private fun ComplexDetailUiState.withPlace(): ComplexDetailUiState {
        val detail = (content as? DetailContent.Loaded)?.detail ?: return this
        val car = car ?: return this
        return copy(place = car.placeOf(detail.lat, detail.lon))
    }

    private companion object {
        const val TAG = "ComplexDetail"
    }
}
