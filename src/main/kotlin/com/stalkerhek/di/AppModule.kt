package com.stalkerhek.di

import com.stalkerhek.data.network.StalkerNetworkRepository
import com.stalkerhek.data.repository.JsonFilterRepository
import com.stalkerhek.data.repository.JsonProfileRepository
import com.stalkerhek.data.repository.JsonUserRepository
import com.stalkerhek.domain.repository.FilterRepository
import com.stalkerhek.domain.repository.ProfileRepository
import com.stalkerhek.domain.repository.StalkerRepository
import com.stalkerhek.domain.repository.UserRepository
import com.stalkerhek.domain.usecase.FilterChannelsUseCase
import com.stalkerhek.domain.usecase.ManageProfilesUseCase
import com.stalkerhek.domain.usecase.StalkerAuthUseCase
import com.stalkerhek.presentation.proxy.StalkerProxyService
import com.stalkerhek.presentation.hls.HlsProxyService
import com.stalkerhek.presentation.web.WebUIService
import com.stalkerhek.presentation.common.LogManager
import org.koin.dsl.module

val appModule = module {
    // Repositories
    single<ProfileRepository> { JsonProfileRepository() }
    single<UserRepository> { JsonUserRepository() }
    single<FilterRepository> { JsonFilterRepository() }
    single<StalkerRepository> { StalkerNetworkRepository() }

    // Use Cases
    single { ManageProfilesUseCase(get(), get()) }
    single { StalkerAuthUseCase(get()) }
    single { FilterChannelsUseCase(get()) }

    // Services
    single { LogManager() }
    single { WebUIService(get(), get(), get()) }
    single { StalkerProxyService(get(), get()) }
    single { HlsProxyService(get(), get()) }
}