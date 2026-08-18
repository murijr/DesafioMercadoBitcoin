package com.desafiomercadobitcoin.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.data.di.dataModule
import com.desafiomercadobitcoin.data.network.CoinMarketCapConfig
import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.ExchangeDetailViewModel
import com.desafiomercadobitcoin.presentation.feature.exchangelist.ExchangeListViewModel
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.test.check.checkModules
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppGraphTest {
    /**
     * A `Application` real ja iniciou o Koin ao subir sob Robolectric; o contexto global e
     * estatico no JVM, entao cada teste precisa comecar do zero.
     */
    @Before
    fun resetGraph() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    /**
     * `checkModules` esta deprecado em favor de `Module.verify()`, mas `verify()` ainda e
     * `@KoinExperimentalAPI` e nao resolve as definicoes que dependem do `Context` do Android.
     * Trocar agora seria substituir um aviso por um teste que nao funciona.
     */
    @Test
    fun `given every module when the graph starts then all definitions are resolvable`() {
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext<Application>())
            modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
        }.checkModules {
            // O `SavedStateHandle` de um `ViewModel` e criado pela plataforma, nao pelo
            // grafo: sem instancia declarada, `checkModules` nao teria como resolve-lo.
            withInstance(SavedStateHandle())
        }
    }

    @Test
    fun `given the started graph when the exchange list view model is requested then it is provided`() {
        val koin =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
            }.koin

        assertNotNull(koin.get<ExchangeListViewModel> { parametersOf(SavedStateHandle()) })
    }

    @Test
    fun `given the started graph when the exchange detail view model is requested then it is provided`() {
        val koin =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
            }.koin

        assertNotNull(koin.get<ExchangeDetailViewModel> { parametersOf(SavedStateHandle()) })
    }

    @Test
    fun `given the started graph when the exchange use case is requested then it is provided`() {
        val koin =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
            }.koin

        assertNotNull(koin.get<GetExchangePageUseCase>())
    }

    @Test
    fun `given the started graph when the exchange detail use cases are requested then they are provided`() {
        val koin =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
            }.koin

        assertNotNull(koin.get<GetExchangeDetailUseCase>())
        assertNotNull(koin.get<GetExchangeCurrenciesUseCase>())
    }

    @Test
    fun `given the started graph when a presentation dependency is requested then it is provided`() {
        val koin =
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext<Application>())
                modules(dataModule(CoinMarketCapConfig(apiKey = "key")), appModule)
            }.koin

        // O cliente HTTP nao e assertavel daqui de proposito: `:data` expoe Ktor como
        // `implementation`, entao `:app` nem compila se tentar toca-lo. checkModules()
        // acima ja garante que ele resolve.
        assertNotNull(koin.get<ResourceProvider>())
    }
}
