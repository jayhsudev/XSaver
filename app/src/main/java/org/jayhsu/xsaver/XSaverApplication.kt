package org.jayhsu.xsaver

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XSaverApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("XSaverApplication", "Application started")
        // 应用初始化代码
    }
}