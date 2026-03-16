package com.sliide.usermanagement

import android.app.Application
import com.sliide.usermanagement.di.androidModule
import com.sliide.usermanagement.di.commonModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(commonModules + androidModule(this@App))
        }
    }
}