package com.desafiomercadobitcoin.presentation.navigation

import android.app.Application
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage
import com.desafiomercadobitcoin.presentation.common.AndroidResourceProvider
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.ExchangeDetailViewModel
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.TAG_DETAIL_BACK
import com.desafiomercadobitcoin.presentation.feature.exchangelist.ExchangeListViewModel
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Versão instrumentada de `AppNavigationTest` (G9) — mesmos cenários da suíte JVM/Robolectric
 * (G7), rodando sobre compositor e dispositivo reais. Mesmos duplos `mockk` dos *use cases* de
 * domínio, registrados em módulo Koin de teste, que a suíte JVM já usa.
 *
 * A pilha é o único lugar que decide o destino visível — então o teste observa a pilha e o
 * que a tela renderiza, e não a implementação da casca.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val getExchangePage = mockk<GetExchangePageUseCase>()
    private val getExchangeDetail = mockk<GetExchangeDetailUseCase>()
    private val getExchangeCurrencies = mockk<GetExchangeCurrenciesUseCase>()
    private var backPressedOwner: OnBackPressedDispatcherOwner? = null

    @Before
    fun startGraph() {
        stopKoin()
        coEvery { getExchangePage.execute(any()) } returns
            Result.success(
                BMExchangePage(
                    items = listOf(exchange(BINANCE_ID, "Binance")),
                    page = 0,
                    hasMore = false,
                ),
            )
        coEvery { getExchangeDetail.execute(BINANCE_ID) } returns Result.success(detail(BINANCE_ID))
        coEvery { getExchangeCurrencies.execute(BINANCE_ID) } returns Result.success(emptyList())
        startKoin {
            modules(
                module {
                    single<ResourceProvider> { AndroidResourceProvider(context) }
                    single { getExchangePage }
                    single { getExchangeDetail }
                    single { getExchangeCurrencies }
                    viewModel { ExchangeListViewModel(get(), get(), get()) }
                    viewModel { ExchangeDetailViewModel(get(), get(), get(), get()) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenTheListWhenAnItemIsSelectedThenTheDetailIsPushedOntoTheStack() {
        val backStack = renderShell()

        composeRule.onNodeWithText("Binance").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(ExchangeListKey, ExchangeDetailKey(BINANCE_ID)), backStack.toList())
        composeRule
            .onNodeWithText(context.getString(R.string.exchange_detail_id_format, BINANCE_ID))
            .assertIsDisplayed()
    }

    @Test
    fun givenTheDetailOnTopWhenTheBackIsTriggeredThenItIsPopped() {
        val backStack = renderShell()
        composeRule.onNodeWithText("Binance").performClick()
        composeRule.waitForIdle()

        backPressedDispatcher().onBackPressed()
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(ExchangeListKey), backStack.toList())
        composeRule.onNodeWithText("Binance").assertIsDisplayed()
    }

    @Test
    fun givenTheDetailOnTopWhenTheBackButtonIsTappedThenItIsPopped() {
        val backStack = renderShell()
        composeRule.onNodeWithText("Binance").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TAG_DETAIL_BACK).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<NavKey>(ExchangeListKey), backStack.toList())
        composeRule.onNodeWithText("Binance").assertIsDisplayed()
    }

    @Test
    fun givenMoreThanOneDestinationWhenStackedThenTheShellHandlesTheBack() {
        val backStack = renderShell()

        composeRule.onNodeWithText("Binance").performClick()
        composeRule.waitForIdle()

        assertEquals(2, backStack.size)
        assertTrue(backPressedDispatcher().hasEnabledCallbacks())
    }

    @Test
    fun givenTheStartDestinationWhenTheBackIsTriggeredThenTheSystemTakesOver() {
        val backStack = renderShell()

        assertEquals(1, backStack.size)
        assertFalse(
            "no destino inicial a casca nao pode consumir o retorno",
            backPressedDispatcher().hasEnabledCallbacks(),
        )
    }

    private fun backPressedDispatcher() =
        composeRule.runOnIdle { requireNotNull(backPressedOwner) }.onBackPressedDispatcher

    private fun renderShell(): NavBackStack<NavKey> {
        lateinit var backStack: NavBackStack<NavKey>
        composeRule.setContent {
            backPressedOwner = LocalOnBackPressedDispatcherOwner.current
            AppTheme {
                backStack = rememberNavBackStack(ExchangeListKey)
                AppNavigation(backStack = backStack)
            }
        }
        composeRule.waitForIdle()
        return backStack
    }

    private fun exchange(
        id: Int,
        name: String,
    ) = BMExchange(
        id = id,
        name = name,
        logoUrl = null,
        spotVolumeUsd = null,
        dateLaunched = null,
    )

    private fun detail(id: Int) =
        BMExchangeDetail(
            id = id,
            name = "Binance",
            logoUrl = null,
            description = null,
            websiteUrl = null,
            makerFee = null,
            takerFee = null,
            dateLaunched = null,
        )
}

private const val BINANCE_ID = 270
