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

/** Qualificador do cliente autenticado da API do provedor. */
val apiHttpClient = named("apiHttpClient")

/** Qualificador do cliente de imagens, que nunca carrega credencial. */
val imageHttpClient = named("imageHttpClient")

/**
 * O engine e o **OkHttp**, engine nativo de Android do Ktor: o cancelamento e thread-safe e
 * nao fecha o socket na thread que cancelou. Como o Coil cancela a carga de uma imagem na
 * main thread quando o item sai da tela, o engine `Android` derrubava o processo com
 * `NetworkOnMainThreadException` ao rolar a lista. Sendo engine do Ktor, nenhuma segunda
 * pilha HTTP entra no grafo.
 *
 * O engine e um `single` compartilhado pelos dois clientes: um unico pool de conexoes e um
 * unico dispatcher no processo. Os `HttpClient` continuam distintos — o que se compartilha
 * e o transporte, nao a configuracao.
 *
 * Módulo Koin de `:data`. Parametrizado pela configuração do provedor, que vem do `:app` —
 * a camada de dados nunca lê `BuildConfig`.
 *
 * Bindings explícitos, sem codegen. Nada de `viewModel { }` aqui.
 */
fun dataModule(config: CoinMarketCapConfig): Module =
    module {
        single { config }
        single<HttpClientEngine> { OkHttp.create() }
        single<HttpClient>(apiHttpClient) { HttpClientFactory.create(get(), get()) }
        single<HttpClient>(imageHttpClient) { HttpClientFactory.createImageClient(get()) }
        single { ExchangeRemoteDataSource(get(apiHttpClient)) }
        // `single` porque o índice memoizado precisa sobreviver entre páginas e entre
        // recriações da tela — é o que impede refazer o `map` a cada lote (D1).
        single<ExchangeRepository> { ExchangeRepositoryImpl(get()) }
        // `factory`: sem estado a memoizar entre chamadas (D3 de `add-exchange-detail`).
        factory<ExchangeDetailRepository> { ExchangeDetailRepositoryImpl(get()) }
    }
