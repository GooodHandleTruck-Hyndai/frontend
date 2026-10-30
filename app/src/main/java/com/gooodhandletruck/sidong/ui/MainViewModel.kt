package com.gooodhandletruck.sidong.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.data.ApiClient
import com.gooodhandletruck.sidong.data.CarDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 화면에 그릴 상태. React의 state 객체 하나라고 보면 돼요. */
data class DriveUiState(
    val speedKmh: Float? = null,
    val gear: String = "-",
    val backendStatus: String = "확인 중…",
    val carError: String? = null,
)

/**
 * 화면 로직 담당. 화면 회전이나 재구성에도 상태가 유지돼요.
 * (React로 치면 상태 + 로직을 담은 store/custom hook 느낌)
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val carRepo = CarDataRepository(app)

    private val _state = MutableStateFlow(DriveUiState())
    val state: StateFlow<DriveUiState> = _state.asStateFlow()

    private var carDataStarted = false

    /** 속도 권한을 받은 뒤에 호출 */
    fun startCarData() {
        if (carDataStarted) return
        carDataStarted = true

        viewModelScope.launch {
            carRepo.speedKmh()
                .catch { e -> _state.update { it.copy(carError = e.message) } }
                .collect { speed -> _state.update { it.copy(speedKmh = speed) } }
        }
        viewModelScope.launch {
            carRepo.gear()
                .catch { e -> _state.update { it.copy(carError = e.message) } }
                .collect { gear -> _state.update { it.copy(gear = gear) } }
        }
    }

    fun onPermissionDenied() {
        _state.update { it.copy(carError = "속도 권한이 거부됐어요") }
    }

    fun checkBackend() {
        _state.update { it.copy(backendStatus = "확인 중…") }
        viewModelScope.launch {
            val result = runCatching { ApiClient.api.health() }
            _state.update {
                it.copy(
                    backendStatus = result.fold(
                        onSuccess = { res -> "연결됨 (${res.status})" },
                        onFailure = { e -> "연결 실패: ${e.message}" },
                    )
                )
            }
        }
    }

    override fun onCleared() {
        carRepo.release()
    }
}
