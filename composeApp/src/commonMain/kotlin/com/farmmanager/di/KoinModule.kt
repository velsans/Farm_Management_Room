package com.farmmanager.di

import com.farmmanager.FarmViewModel
import com.farmmanager.data.FarmDao
import com.farmmanager.data.FarmDatabase
import com.farmmanager.data.FarmRepository
import com.farmmanager.data.buildFarmDatabase
import com.farmmanager.data.getDatabaseBuilder
import com.farmmanager.export.ExcelManager
import com.farmmanager.platform.PlatformFileHandler
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { buildFarmDatabase(getDatabaseBuilder()) }
    single<FarmDao> { get<FarmDatabase>().farmDao() }
    single { FarmRepository(get()) }
    single { ExcelManager() }
    single { PlatformFileHandler() }
    viewModel { FarmViewModel(get(), get(), get()) }
}
