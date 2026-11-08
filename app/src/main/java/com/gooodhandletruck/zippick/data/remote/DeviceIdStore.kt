package com.gooodhandletruck.zippick.data.remote

import android.content.Context
import androidx.core.content.edit

/** 기기 등록으로 받은 익명 ID를 기억하는 곳. 앱은 [DeviceIdStore]를 쓰고, 단위 테스트는 메모리에 둔다 */
interface DeviceIdStorage {
    /** 아직 등록하지 않았으면 null */
    var anonymousId: String?
}

/** 익명 ID를 이 기기(SharedPreferences)에 기억한다. 앱 데이터를 지우면 사라지고, 다음에 다시 등록한다 */
class DeviceIdStore(context: Context) : DeviceIdStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 헤더를 붙이는 OkHttp 스레드와 등록하는 코루틴이 함께 읽고 쓴다
    @Volatile
    override var anonymousId: String? = prefs.getString(KEY_ANONYMOUS_ID, null)
        set(value) {
            field = value
            prefs.edit { if (value == null) remove(KEY_ANONYMOUS_ID) else putString(KEY_ANONYMOUS_ID, value) }
        }

    private companion object {
        const val PREFS_NAME = "device"
        const val KEY_ANONYMOUS_ID = "anonymous_id"
    }
}
