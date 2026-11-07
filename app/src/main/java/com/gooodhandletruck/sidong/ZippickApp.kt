package com.gooodhandletruck.sidong

import android.app.Application
import com.gooodhandletruck.sidong.data.AppContainer

/** 앱 프로세스에 하나. ViewModel은 [container]에서 저장소를 받아 쓴다. */
class ZippickApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
