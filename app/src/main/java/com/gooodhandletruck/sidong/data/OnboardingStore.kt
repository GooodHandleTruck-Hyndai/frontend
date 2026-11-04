package com.gooodhandletruck.sidong.data

import android.content.Context
import androidx.core.content.edit

/** 첫 실행(S-01·S-02)을 끝냈는지 기억한다. 끝냈으면 다음 실행부터 S-03에서 시작한다. */
class OnboardingStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isDone: Boolean
        get() = prefs.getBoolean(KEY_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_DONE, value) }

    private companion object {
        const val PREFS_NAME = "onboarding"
        const val KEY_DONE = "done"
    }
}
