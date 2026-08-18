package com.desafiomercadobitcoin.di

import com.desafiomercadobitcoin.presentation.common.AndroidResourceProvider
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Wire-up do `:app`. É o único lugar do módulo que pode tocar em `:data` — `presentation/`
 * nunca importa a camada de dados.
 */
val appModule =
    module {
        single<ResourceProvider> { AndroidResourceProvider(androidContext()) }
    }
