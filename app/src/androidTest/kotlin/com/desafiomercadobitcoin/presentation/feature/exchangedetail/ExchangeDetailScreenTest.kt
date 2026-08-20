package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class ExchangeDetailScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val events = mutableListOf<ExchangeDetailEvent>()
    private var backClicks = 0

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenAnyStateWhenRenderingThenTheBackButtonIsDisplayed() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_BACK).assertIsDisplayed()
    }

    @Test
    fun givenTheBackButtonWhenItIsTappedThenTheBackActionFires() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_BACK).performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun givenTheDetailIsLoadingWhenRenderingThenTheLoadingIndicatorIsDisplayed() {
        render(VMExchangeDetailState(isLoadingDetail = true))

        composeRule.onNodeWithTag(TAG_DETAIL_LOADING).assertIsDisplayed()
    }

    @Test
    fun givenTheDetailFailedWhenRenderingThenTheMessageAndTheRetryActionAreDisplayed() {
        render(VMExchangeDetailState(detailErrorMessage = "falhou"))

        composeRule.onNodeWithTag(TAG_DETAIL_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("falhou").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).assertIsDisplayed()
    }

    @Test
    fun givenTheDetailErrorWhenTheRetryIsTappedThenTheDetailRetryEventIsEmitted() {
        render(VMExchangeDetailState(detailErrorMessage = "falhou"))

        composeRule.onNodeWithTag(TAG_DETAIL_RETRY).performClick()

        assertEquals(listOf(ExchangeDetailEvent.RetryDetailRequested), events)
    }

    @Test
    fun givenTheExchangeIsNotFoundWhenRenderingThenTheMessageIsDisplayedWithoutRetry() {
        render(VMExchangeDetailState(detailErrorMessage = "nao encontrada", isDetailNotFound = true))

        composeRule.onNodeWithTag(TAG_DETAIL_NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithText("nao encontrada").assertIsDisplayed()
    }

    @Test
    fun givenTheDetailWhenRenderingThenItsFieldsAreDisplayed() {
        render(VMExchangeDetailState(detail = detail()))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                context.getString(R.string.exchange_detail_id_format, EXCHANGE_ID),
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Uma exchange").assertIsDisplayed()
    }

    @Test
    fun givenCurrenciesLoadingWhenRenderingThenTheCurrenciesLoadingIndicatorIsDisplayed() {
        render(VMExchangeDetailState(detail = detail(), isLoadingCurrencies = true))

        scrollTo(hasTestTag(TAG_CURRENCIES_LOADING))
        composeRule.onNodeWithTag(TAG_CURRENCIES_LOADING).assertIsDisplayed()
    }

    @Test
    fun givenNoCurrenciesWhenRenderingThenTheEmptyMessageIsDisplayed() {
        render(VMExchangeDetailState(detail = detail(), currencies = emptyList()))

        scrollTo(hasTestTag(TAG_CURRENCIES_EMPTY))
        composeRule.onNodeWithTag(TAG_CURRENCIES_EMPTY).assertIsDisplayed()
    }

    @Test
    fun givenCurrenciesWhenRenderingThenEachOneIsDisplayed() {
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
    fun givenCurrenciesWithDuplicateNamesWhenRenderingThenTheListDoesNotCrash() {
        render(
            VMExchangeDetailState(
                detail = detail(),
                currencies =
                    listOf(
                        VMCurrency(name = "Avantis", priceLabel = "US$ 1,20"),
                        VMCurrency(name = "Avantis", priceLabel = "US$ 1,21"),
                    ),
            ),
        )

        scrollTo(hasText("Avantis"))
        composeRule.onAllNodesWithText("Avantis").fetchSemanticsNodes()
    }

    @Test
    fun givenCurrenciesFailedWhenRenderingThenTheMessageAndTheRetryActionAreDisplayed() {
        render(VMExchangeDetailState(detail = detail(), currenciesErrorMessage = "moedas falharam"))

        scrollTo(hasTestTag(TAG_CURRENCIES_ERROR))
        composeRule.onNodeWithTag(TAG_CURRENCIES_ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("moedas falharam").assertIsDisplayed()
    }

    @Test
    fun givenCurrenciesErrorWhenTheRetryIsTappedThenTheCurrenciesRetryEventIsEmitted() {
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
