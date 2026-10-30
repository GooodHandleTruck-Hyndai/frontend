package com.gooodhandletruck.sidong

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.gooodhandletruck.sidong.ui.DriveScreen
import com.gooodhandletruck.sidong.ui.DriveTheme
import com.gooodhandletruck.sidong.ui.MainViewModel

/** 앱 진입점. React의 index.tsx + App 렌더링 부분이에요. */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val speedPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.startCarData() else viewModel.onPermissionDenied()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 속도 권한은 런타임에 사용자 허락을 받아야 해요
        val granted = ContextCompat.checkSelfPermission(this, PERMISSION_SPEED) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startCarData()
        } else {
            speedPermissionLauncher.launch(PERMISSION_SPEED)
        }

        viewModel.checkBackend()

        setContent {
            DriveTheme {
                DriveScreen(viewModel)
            }
        }
    }

    private companion object {
        const val PERMISSION_SPEED = "android.car.permission.CAR_SPEED"
    }
}
