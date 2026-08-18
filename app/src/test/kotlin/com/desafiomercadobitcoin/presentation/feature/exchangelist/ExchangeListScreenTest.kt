package com.desafiomercadobitcoin.presentation.feature.exchangelist

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExchangeListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val events = mutableListOf<ExchangeListEvent>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given the initial loading state when rendering then the loading indicator is displayed`() {
        render(VMExchangeListState(isLoading = true))

        composeRule.onNodeWithTag(TAG_INITIAL_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given content when rendering then every exchange name is displayed`() {
        render(VMExchangeListState(items = listOf(exchange(1, "Binance"), exchange(2, "OKX"))))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText("OKX").assertIsDisplayed()
    }

    @Test
    fun `given an exchange without volume when rendering then the unavailable text is displayed`() {
        val unavailable = context.getString(R.string.exchange_field_unavailable)
        render(VMExchangeListState(items = listOf(exchange(1, "Binance", volumeLabel = unavailable))))

        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun `given an empty catalog when rendering then the empty message is displayed`() {
        render(VMExchangeListState(items = emptyList(), hasMore = false))

        composeRule.onNodeWithText(context.getString(R.string.exchange_list_empty)).assertIsDisplayed()
    }

    @Test
    fun `given an error when rendering then the message and the retry action are displayed`() {
        render(VMExchangeListState(errorMessage = "falhou"))

        composeRule.onNodeWithText("falhou").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).assertIsDisplayed()
    }

    @Test
    fun `given the error state when the retry is tapped then the retry event is emitted`() {
        render(VMExchangeListState(errorMessage = "falhou"))

        composeRule.onNodeWithText(context.getString(R.string.exchange_list_retry)).performClick()

        assertEquals(listOf(ExchangeListEvent.RetryRequested), events)
    }

    @Test
    fun `given a batch in flight when rendering then the paging indicator is displayed`() {
        render(VMExchangeListState(items = listOf(exchange(1, "Binance")), isLoadingMore = true))

        composeRule.onNodeWithTag(TAG_PAGING_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given a failed batch when rendering then the content is kept alongside the message`() {
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
    fun `given content when an item is tapped then the selection event carries its id`() {
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
