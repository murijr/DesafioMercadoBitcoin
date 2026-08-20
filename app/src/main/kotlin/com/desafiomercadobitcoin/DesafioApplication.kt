package com.desafiomercadobitcoin

import android.app.Application
import android.os.StrictMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.size.Precision
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

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .precision(Precision.EXACT)
            .crossfade(IMAGE_CROSSFADE_DURATION_MILLIS)
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = { GlobalContext.get().get<HttpClient>(imageHttpClient) },
                    ),
                )
            }.build()
}

private const val IMAGE_CROSSFADE_DURATION_MILLIS: Int = 200
