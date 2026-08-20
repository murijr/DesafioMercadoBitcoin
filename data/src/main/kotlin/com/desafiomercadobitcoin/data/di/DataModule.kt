package com.desafiomercadobitcoin.data.di

import com.desafiomercadobitcoin.data.exchange.ExchangeDetailRepositoryImpl
import com.desafiomercadobitcoin.data.exchange.ExchangeRemoteDataSource
import com.desafiomercadobitcoin.data.exchange.ExchangeRepositoryImpl
import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.data.network.HttpClientFactory
import com.desafiomercadobitcoin.domain.exchange.ExchangeDetailRepository
import com.desafiomercadobitcoin.domain.exchange.ExchangeRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val apiHttpClient = named("apiHttpClient")

val imageHttpClient = named("imageHttpClient")

fun dataModule(config: CoinMarketCapConfig): Module =
    module {
        single { config }
        single<HttpClientEngine> { OkHttp.create() }
        single<HttpClient>(apiHttpClient) { HttpClientFactory.create(get(), get()) }
        single<HttpClient>(imageHttpClient) { HttpClientFactory.createImageClient(get()) }
        single { ExchangeRemoteDataSource(get(apiHttpClient)) }
        single<ExchangeRepository> { ExchangeRepositoryImpl(get()) }
        factory<ExchangeDetailRepository> { ExchangeDetailRepositoryImpl(get()) }
    }
