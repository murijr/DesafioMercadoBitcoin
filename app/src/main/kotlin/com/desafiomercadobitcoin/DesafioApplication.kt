package com.desafiomercadobitcoin

import android.app.Application
import android.os.StrictMode
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
        installStrictModeThreadPolicy()
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
     * G10 — I/O na *main thread* mata o processo em vez de virar *jank* silencioso que só
     * aparece no aparelho do usuário. Instalada antes do grafo, para que a própria montagem
     * do Koin esteja sob a política.
     *
     * Só em depuração: em release, `penaltyDeath` transformaria problema de performance em
     * falha para o usuário final. `penaltyLog` acompanha `penaltyDeath` porque é ele que
     * carrega o rastro de pilha — sem log, a morte do processo não diz onde foi o I/O.
     *
     * Violação se corrige **no código** (tirar o I/O da *main thread*, ou delimitar no ponto
     * exato a chamada de plataforma que comprovadamente não sai dela), nunca removendo uma
     * detecção nem rebaixando a penalidade.
     */
    private fun installStrictModeThreadPolicy() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDeath()
                .build(),
        )
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
