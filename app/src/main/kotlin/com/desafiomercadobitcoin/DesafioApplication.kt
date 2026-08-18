package com.desafiomercadobitcoin

import android.app.Application
import com.desafiomercadobitcoin.data.di.dataModule
import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DesafioApplication : Application() {
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
}
