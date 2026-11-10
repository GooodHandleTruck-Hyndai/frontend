package com.gooodhandletruck.zippick.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.zippick.data.remote.InterestConditionDto
import com.gooodhandletruck.zippick.data.remote.RegionDto
import com.gooodhandletruck.zippick.data.remote.ServerClient
import com.gooodhandletruck.zippick.data.remote.decode
import com.gooodhandletruck.zippick.data.remote.toServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * S-02·S-07에서 바꾼 알림 조건을 서버 관심 조건(PUT /api/v1/interest-conditions, B 16번)으로 보낸다.
 * 서버는 이 조건으로 관심 건물의 새 거래 알림(S-10)을 만든다.
 *
 * 앱의 기준은 여전히 이 기기에 기억한 조건([AlertSettingsRepository])이다. 주행 중 자동 알림은 기기 조건으로 거르고,
 * 서버에는 바뀔 때마다 덮어써 보낸다. 보내지 못하면 다음에 바꿀 때나 앱을 다시 켤 때 다시 보낸다.
 *
 * 관심 지역: S-02에 지역 고르기가 없어서, 보낼 때의 차 위치로 시군구 코드를 받는다(GET /api/v1/regions, B 2번).
 * 위치를 못 받으면 마지막으로 받은 코드를, 그것도 없으면 서버에 데이터가 있는 마포구를 쓴다.
 */
class InterestConditionSync(
    context: Context,
    private val server: ServerClient,
    private val alertSettings: AlertSettingsRepository,
    private val locationRepository: LocationRepository,
    private val scope: CoroutineScope
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 마지막으로 서버가 받아 준 값. 같은 값을 다시 보내지 않는다 */
    private var lastSent: InterestConditionDto? = null

    fun start() {
        scope.launch {
            alertSettings.settings.collectLatest { settings ->
                val conditions = settings.conditions ?: return@collectLatest
                delay(DEBOUNCE_MS) // 칩을 여러 번 눌러도 마지막 값 하나만 보낸다
                push(conditions, settings.enabled)
            }
        }
    }

    private suspend fun push(conditions: AlertConditions, enabled: Boolean) {
        try {
            val body = conditions.toServer(regionCode(), enabled)
            if (body == lastSent) return
            server.put(PATH, AppJson.encodeToJsonElement(InterestConditionDto.serializer(), body))
            lastSent = body
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "관심 조건을 서버에 보내지 못했어요", e)
        }
    }

    private suspend fun regionCode(): String {
        val location = withTimeoutOrNull(LOCATION_WAIT_MS) {
            locationRepository.state.filterIsInstance<LocationState.Found>().first().location
        }
        if (location != null) {
            try {
                val region = server.get(REGION_PATH, mapOf("lat" to location.lat, "lon" to location.lon))
                    .decode<RegionDto>()
                prefs.edit { putString(KEY_REGION, region.lawdCd) }
                return region.lawdCd
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "지금 위치의 시군구 코드를 받지 못했어요", e)
            }
        }
        return prefs.getString(KEY_REGION, null) ?: DEFAULT_REGION
    }

    private companion object {
        const val TAG = "InterestConditionSync"
        const val PATH = "api/v1/interest-conditions"
        const val REGION_PATH = "api/v1/regions"
        const val PREFS_NAME = "interest_sync"
        const val KEY_REGION = "region_code"

        /** 마포구. 지금 서버에는 마포구 실거래 데이터만 있다 */
        const val DEFAULT_REGION = "11440"
        const val DEBOUNCE_MS = 500L
        const val LOCATION_WAIT_MS = 10_000L
    }
}
