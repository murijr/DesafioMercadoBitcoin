package com.desafiomercadobitcoin.presentation.feature.exchangelist

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
class ExchangeListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val events = mutableListOf<ExchangeListEvent>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun givenTheInitialLoadingStateWhenRenderingThenTheLoadingIndicatorIsDisplayed() {
        render(VMExchangeListState(isLoading = true))

        composeRule.onNodeWithTag(TAG_INITIAL_LOADING).assertIsDisplayed()
    }

    @Test
    fun givenContentWhenRenderingThenEveryExchangeNameIsDisplayed() {
        render(VMExchangeListState(items = listOf(exchange(1, "Binance"), exchange(2, "OKX"))))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText("OKX").assertIsDisplayed()
    }

    @Test
    fun givenAnExchangeWithoutVolumeWhenRenderingThenTheUnavailableTextIsDisplayed() {
        val unavailable = context.getString(R.string.exchange_field_unavailable)
        render(VMExchangeListState(items = listOf(exchange(1, "Binance", volumeLabel = unavailable))))

        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenAnEmptyCatalogWhenRenderingThenTheEmptyMessageIsDisplayed() {
        render(VMExchangeListState(items = emptyList(), hasMore = false))

        composeRule.onNodeWithText(context.getString(R.string.exchange_list_empty)).assertIsDisplayed()
    }

    @Test
    fun givenAnErrorWhenRenderingThenTheMessageAndTheRetryActionAreDisplayed() {
        render(VMExchangeListState(errorMessage = "falhou"))

        composeRule.onNodeWithText("falhou").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).assertIsDisplayed()
    }

    @Test
    fun givenTheErrorStateWhenTheRetryIsTappedThenTheRetryEventIsEmitted() {
        render(VMExchangeListState(errorMessage = "falhou"))

        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).performClick()

        assertEquals(listOf(ExchangeListEvent.RetryRequested), events)
    }

    @Test
    fun givenABatchInFlightWhenRenderingThenThePagingIndicatorIsDisplayed() {
        render(VMExchangeListState(items = listOf(exchange(1, "Binance")), isLoadingMore = true))

        composeRule.onNodeWithTag(TAG_PAGING_LOADING).assertIsDisplayed()
    }

    @Test
    fun givenAFailedBatchWhenRenderingThenTheContentIsKeptAlongsideTheMessage() {
        render(
            VMExchangeListState(
                items = listOf(exchange(1, "Binance")),
                pagingErrorMessage = "lote falhou",
            ),
        )

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText("lote falhou").assertIsDisplayed()
    }

    @Test
    fun givenContentWhenAnItemIsTappedThenTheSelectionEventCarriesItsId() {
        render(VMExchangeListState(items = listOf(exchange(MERCADO_BITCOIN_ID, "Mercado Bitcoin"))))

        composeRule.onNodeWithText("Mercado Bitcoin").performClick()

        assertEquals(
            listOf(ExchangeListEvent.ExchangeSelected(MERCADO_BITCOIN_ID)),
            events.filterNot { it is ExchangeListEvent.NextPageRequested },
        )
    }

    private fun render(state: VMExchangeListState) {
        composeRule.setContent {
            AppTheme {
                ExchangeListScreen(state = state, onEvent = { events += it })
            }
        }
    }

    private fun exchange(
        id: Int,
        name: String,
        volumeLabel: String = "US$ 1,2 bi",
    ) = VMExchange(
        id = id,
        name = name,
        logoUrl = null,
        volumeLabel = volumeLabel,
        launchDateLabel = "14 de jul. de 2017",
    )
}

private const val MERCADO_BITCOIN_ID = 302
