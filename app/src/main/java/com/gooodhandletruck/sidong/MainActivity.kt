package com.gooodhandletruck.sidong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gooodhandletruck.sidong.ui.AppRoot
import com.gooodhandletruck.sidong.ui.AppViewModel
import com.gooodhandletruck.sidong.ui.theme.AppTheme

/**
 * 앱의 하나뿐인 Activity. 정차 화면과 주행 화면을 모두 이 안에서 바꿔 그린다.
 * 주행 중에도 보이므로 매니페스트에서 distractionOptimized를 켜 둔다.
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
    }

    override fun onResume() {
        super.onResume()
        // 권한 창이 닫히거나 다른 앱에서 돌아오면 위치 권한을 다시 확인한다
        viewModel.refreshLocationPermission()
    }
}
