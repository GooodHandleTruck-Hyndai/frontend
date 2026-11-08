package com.gooodhandletruck.zippick

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gooodhandletruck.zippick.data.DrivingStateRepository
import com.gooodhandletruck.zippick.ui.AppRoot
import com.gooodhandletruck.zippick.ui.AppViewModel
import com.gooodhandletruck.zippick.ui.theme.AppTheme

/**
 * 앱의 하나뿐인 Activity. 정차 화면과 주행 화면을 모두 이 안에서 바꿔 그린다.
 * 주행 중에도 보이므로 매니페스트에서 distractionOptimized를 켜 둔다.
 * singleTop이라 아래 독의 앱 아이콘을 다시 눌러도 새로 쌓지 않고 보던 화면을 그대로 보여 준다.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppRoot(viewModel)
            }
        }
        applyDebugIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 권한 창이 닫히거나 다른 앱에서 돌아오면 위치 권한을 다시 확인한다
        viewModel.refreshLocationPermission()
    }

    /**
     * 디버그 빌드에서만: 에뮬레이터에서 adb로 주행 상태를 흉내 낸다.
     * adb shell am start --user 10 -n com.gooodhandletruck.zippick/.MainActivity --ez debug_driving true (정차는 false)
     */
    private fun applyDebugIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent?.hasExtra(EXTRA_DEBUG_DRIVING) != true) return
        val driving = intent.getBooleanExtra(EXTRA_DEBUG_DRIVING, false)
        Log.i(TAG, "디버그 주행 상태: $driving")
        DrivingStateRepository.debugOverride.value = driving
    }

    private companion object {
        const val TAG = "MainActivity"
        const val EXTRA_DEBUG_DRIVING = "debug_driving"
    }
}
