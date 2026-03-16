package com.sliide.usermanagement.di

import android.content.Context
import com.sliide.usermanagement.db.DatabaseDriverFactory
import org.koin.dsl.module

fun androidModule(context: Context) = module {
    single { DatabaseDriverFactory(context) }
}
