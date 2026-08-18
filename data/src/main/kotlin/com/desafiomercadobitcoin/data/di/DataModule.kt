package com.desafiomercadobitcoin.data.di

import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.data.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo Koin de `:data`. Parametrizado pela configuração do provedor, que vem do `:app` —
 * a camada de dados nunca lê `BuildConfig`.
 *
 * Bindings explícitos, sem codegen. Nada de `viewModel { }` aqui.
 */
fun dataModule(config: CoinMarketCapConfig): Module =
    module {
        single { config }
        single<HttpClient> { HttpClientFactory.create(Android.create(), get()) }
    }
