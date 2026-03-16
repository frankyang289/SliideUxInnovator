package com.sliide.usermanagement

import com.sliide.usermanagement.db.DatabaseDriverFactory
import com.sliide.usermanagement.di.commonModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin() {
    startKoin {
        modules(
            commonModules + module {
                single { DatabaseDriverFactory() }
            }
        )
    }
}
