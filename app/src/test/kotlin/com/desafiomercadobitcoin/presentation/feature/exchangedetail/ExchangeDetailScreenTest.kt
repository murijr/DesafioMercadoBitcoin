package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import android.app.Application
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExchangeDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val events = mutableListOf<ExchangeDetailEvent>()
    private var backClicks = 0

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given any state when rendering then the back button is displayed`() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_BACK).assertIsDisplayed()
    }

    @Test
    fun `given the back button when it is tapped then the back action fires`() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_BACK).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun `given the detail is loading when rendering then the loading indicator is displayed`() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given the detail failed when rendering then the message and the retry action are displayed`() {
        render(VMExchangeDetailState(detailErrorMessage = "falhou"))

        composeRule.onNodeWithTag(TAG_DETAIL_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("falhou").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).assertIsDisplayed()
    }

    @Test
    fun `given the detail error when the retry is tapped then the detail retry event is emitted`() {
        render(VMExchangeDetailState(detailErrorMessage = "falhou"))

        composeRule.onNodeWithTag(TAG_DETAIL_RETRY).performClick()

        assertEquals(listOf(ExchangeDetailEvent.RetryDetailRequested), events)
    }

    @Test
    fun `given the exchange is not found when rendering then the message is displayed without retry`() {
        render(VMExchangeDetailState(detailErrorMessage = "nao encontrada", isDetailNotFound = true))

        composeRule.onNodeWithTag(TAG_DETAIL_NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithText("nao encontrada").assertIsDisplayed()
    }

    @Test
    fun `given the detail when rendering then its fields are displayed`() {
        render(VMExchangeDetailState(detail = detail()))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                context.getString(R.string.exchange_detail_id_format, EXCHANGE_ID),
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Uma exchange").assertIsDisplayed()
    }

    @Test
    fun `given currencies loading when rendering then the currencies loading indicator is displayed`() {
        render(VMExchangeDetailState(detail = detail(), isLoadingCurrencies = true))

        scrollTo(hasTestTag(TAG_CURRENCIES_LOADING))
        composeRule.onNodeWithTag(TAG_CURRENCIES_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given no currencies when rendering then the empty message is displayed`() {
        render(VMExchangeDetailState(detail = detail(), currencies = emptyList()))

        scrollTo(hasTestTag(TAG_CURRENCIES_EMPTY))
        composeRule.onNodeWithTag(TAG_CURRENCIES_EMPTY).assertIsDisplayed()
    }

    @Test
    fun `given currencies when rendering then each one is displayed`() {
        render(
            VMExchangeDetailState(
                detail = detail(),
                currencies = listOf(VMCurrency(name = "Bitcoin", priceLabel = "US$ 45.000,00")),
            ),
        )

        scrollTo(hasText("Bitcoin"))
        composeRule.onNodeWithText("Bitcoin").assertIsDisplayed()
        composeRule.onNodeWithText("US$ 45.000,00").assertIsDisplayed()
    }

    @Test
    fun `given currencies failed when rendering then the message and the retry action are displayed`() {
        render(VMExchangeDetailState(detail = detail(), currenciesErrorMessage = "moedas falharam"))

        scrollTo(hasTestTag(TAG_CURRENCIES_ERROR))
        composeRule.onNodeWithTag(TAG_CURRENCIES_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("moedas falharam").assertIsDisplayed()
    }

    @Test
    fun `given currencies error when the retry is tapped then the currencies retry event is emitted`() {
        render(VMExchangeDetailState(detail = detail(), currenciesErrorMessage = "moedas falharam"))

        scrollTo(hasTestTag(TAG_CURRENCIES_RETRY))
        composeRule.onNodeWithTag(TAG_CURRENCIES_RETRY).performClick()

        assertEquals(listOf(ExchangeDetailEvent.RetryCurrenciesRequested), events)
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        composeRule.onNodeWithTag(TAG_DETAIL_CONTENT).performScrollToNode(matcher)
    }

    private fun render(state: VMExchangeDetailState) {
        composeRule.setContent {
            AppTheme {
                ExchangeDetailScreen(
                    state = state,
                    onEvent = { events += it },
                    onBackClick = { backClicks++ },
                )
            }
        }
    }

    private fun detail() =
        VMExchangeDetail(
            id = EXCHANGE_ID,
            name = "Binance",
            logoUrl = null,
            descriptionLabel = "Uma exchange",
            websiteLabel = "https://exchange.example",
            makerFeeLabel = "2%",
            takerFeeLabel = "4%",
            launchDateLabel = "14 de jul. de 2017",
        )
}

private const val EXCHANGE_ID = 270
