package com.farmmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.farmmanager.platform.currentActivity
import com.farmmanager.platform.initAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initAndroidContext(applicationContext)
        currentActivity = this
        enableEdgeToEdge()
        setContent {
            App(platformUiActions = rememberAndroidPlatformUiActions())
        }
    }

    override fun onDestroy() {
        if (currentActivity === this) {
            currentActivity = null
        }
        super.onDestroy()
    }
}
