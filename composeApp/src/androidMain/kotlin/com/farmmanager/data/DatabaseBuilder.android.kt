package com.farmmanager.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.farmmanager.platform.androidContext

actual fun getDatabaseBuilder(): RoomDatabase.Builder<FarmDatabase> {
    val context = androidContext()
    return Room.databaseBuilder<FarmDatabase>(
        context = context,
        name = context.getDatabasePath("farm_manager.db").absolutePath,
    )
}
