package com.desafiomercadobitcoin.di

import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.presentation.common.AndroidResourceProvider
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangelist.ExchangeListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Wire-up do `:app`. É o único lugar do módulo que pode tocar em `:data` — `presentation/`
 * nunca importa a camada de dados.
 */
val appModule =
    module {
        single<ResourceProvider> { AndroidResourceProvider(androidContext()) }
        factory { GetExchangePageUseCase(get()) }
        viewModel { ExchangeListViewModel(get(), get(), get()) }
    }
