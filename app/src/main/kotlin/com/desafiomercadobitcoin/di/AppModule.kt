package com.desafiomercadobitcoin.di

import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.presentation.common.AndroidResourceProvider
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.ExchangeDetailViewModel
import com.desafiomercadobitcoin.presentation.feature.exchangelist.ExchangeListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single<ResourceProvider> { AndroidResourceProvider(androidContext()) }
        factory { GetExchangePageUseCase(get()) }
        factory { GetExchangeDetailUseCase(get()) }
        factory { GetExchangeCurrenciesUseCase(get()) }
        viewModel { ExchangeListViewModel(get(), get(), get()) }
        viewModel { ExchangeDetailViewModel(get(), get(), get(), get()) }
    }
