package com.farmmanager.platform

import android.content.Context

lateinit var androidContext: Context
    private set

fun initAndroidContext(context: Context) {
    androidContext = context.applicationContext
}

fun androidContext(): Context = androidContext
