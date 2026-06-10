package com.farmmanager

import android.app.Application
import com.farmmanager.platform.initAndroidContext

class FarmManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAndroidContext(this)
    }
}
