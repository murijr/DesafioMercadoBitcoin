package com.desafiomercadobitcoin

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.desafiomercadobitcoin.data.di.dataModule
import com.desafiomercadobitcoin.data.di.imageHttpClient
import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.di.appModule
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DesafioApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DesafioApplication)
            modules(
                dataModule(
                    CoinMarketCapConfig(
                        apiKey = BuildConfig.CMC_API_KEY,
                        isDebug = BuildConfig.DEBUG,
                    ),
                ),
                appModule,
            )
        }
    }

    /**
     * O Coil carrega as imagens pelo cliente **sem** credencial: a chave da API nunca
     * acompanha requisição a host de imagem (D5). O cliente é resolvido preguiçosamente
     * porque o Coil pode montar o carregador antes de alguém tocar no grafo.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = { GlobalContext.get().get<HttpClient>(imageHttpClient) },
                    ),
                )
            }.build()
}
