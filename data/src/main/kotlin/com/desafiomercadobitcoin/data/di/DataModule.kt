package com.desafiomercadobitcoin.data.di

import com.desafiomercadobitcoin.data.exchange.ExchangeDetailRepositoryImpl
import com.desafiomercadobitcoin.data.exchange.ExchangeRemoteDataSource
import com.desafiomercadobitcoin.data.exchange.ExchangeRepositoryImpl
import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.data.network.HttpClientFactory
import com.desafiomercadobitcoin.domain.exchange.ExchangeDetailRepository
import com.desafiomercadobitcoin.domain.exchange.ExchangeRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualificador do cliente autenticado da API do provedor. */
val apiHttpClient = named("apiHttpClient")

/** Qualificador do cliente de imagens, que nunca carrega credencial. */
val imageHttpClient = named("imageHttpClient")

/**
 * O engine e o **CIO**, e nao o `Android`: o engine `Android` fecha o socket dentro do
 * proprio handler de cancelamento, na thread que cancelou. Como o Coil cancela a carga de
 * uma imagem na main thread quando o item sai da tela, rolar a lista derrubava o processo
 * com `NetworkOnMainThreadException`. O CIO e nao bloqueante e continua sendo Ktor —
 * nenhuma segunda pilha HTTP entra no grafo.
 *
 * Módulo Koin de `:data`. Parametrizado pela configuração do provedor, que vem do `:app` —
 * a camada de dados nunca lê `BuildConfig`.
 *
 * Bindings explícitos, sem codegen. Nada de `viewModel { }` aqui.
 */
fun dataModule(config: CoinMarketCapConfig): Module =
    module {
        single { config }
        single<HttpClient>(apiHttpClient) { HttpClientFactory.create(CIO.create(), get()) }
        single<HttpClient>(imageHttpClient) { HttpClientFactory.createImageClient(CIO.create()) }
        single { ExchangeRemoteDataSource(get(apiHttpClient)) }
        // `single` porque o índice memoizado precisa sobreviver entre páginas e entre
        // recriações da tela — é o que impede refazer o `map` a cada lote (D1).
        single<ExchangeRepository> { ExchangeRepositoryImpl(get()) }
        // `factory`: sem estado a memoizar entre chamadas (D3 de `add-exchange-detail`).
        factory<ExchangeDetailRepository> { ExchangeDetailRepositoryImpl(get()) }
    }
