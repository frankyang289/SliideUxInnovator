package com.sliide.usermanagement.di

import com.sliide.usermanagement.data.network.UserApiService
import com.sliide.usermanagement.data.network.createHttpClient
import com.sliide.usermanagement.data.repository.UserRepositoryImpl
import com.sliide.usermanagement.db.DatabaseDriverFactory
import com.sliide.usermanagement.db.UserDatabase
import com.sliide.usermanagement.domain.repository.UserRepository
import com.sliide.usermanagement.goRestToken
import com.sliide.usermanagement.ui.UserViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { UserApiService(get(), goRestToken()) }
}

val databaseModule = module {
    single { get<DatabaseDriverFactory>().createDriver() }
    single { UserDatabase(get()) }
}

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

val viewModelModule = module {
    viewModel { UserViewModel(get()) }
}

val commonModules = listOf(
    networkModule,
    databaseModule,
    repositoryModule,
    viewModelModule
)
