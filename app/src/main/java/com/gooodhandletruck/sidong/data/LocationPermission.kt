package com.gooodhandletruck.sidong.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * 플레오스 위치(FusedLocation) 권한. 위치 서비스 앱(ai.pleos.playground.service.location.fused)이 정의한
 * 위험(dangerous) 권한이라, 매니페스트에 적는 것만으로는 부족하고 실행 중에 물어야 한다.
 */
object LocationPermission {
    const val NAME = "pleos.car.permission.FUSED_LOCATION"

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, NAME) == PackageManager.PERMISSION_GRANTED
}
