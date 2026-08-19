package com.desafiomercadobitcoin.presentation.feature.exchangedetail.components

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `CurrencyListItem` isolado, sem montar `ExchangeDetailScreen` (G9): confirma que o componente
 * renderiza nome e preço, inclusive quando o preço é zero ou está ausente, em dispositivo/emulador
 * real.
 */
@RunWith(AndroidJUnit4::class)
class CurrencyListItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun givenAPriceWhenRenderingThenNameAndPriceAreDisplayed() {
        render(VMCurrency(name = "Bitcoin", priceLabel = "US$ 45.000,00"))

        composeRule.onNodeWithText("Bitcoin").assertIsDisplayed()
        composeRule.onNodeWithText("US$ 45.000,00").assertIsDisplayed()
    }

    @Test
    fun givenAZeroPriceWhenRenderingThenTheZeroValueIsDisplayed() {
        render(VMCurrency(name = "Bitcoin", priceLabel = "US$ 0,00"))

        composeRule.onNodeWithText("Bitcoin").assertIsDisplayed()
        composeRule.onNodeWithText("US$ 0,00").assertIsDisplayed()
    }

    @Test
    fun givenNoPriceWhenRenderingThenTheUnavailableTextIsDisplayed() {
        val unavailable = context.getString(R.string.exchange_field_unavailable)
        render(VMCurrency(name = "Bitcoin", priceLabel = unavailable))

        composeRule.onNodeWithText("Bitcoin").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    private fun render(currency: VMCurrency) {
        composeRule.setContent {
            AppTheme {
                CurrencyListItem(currency = currency)
            }
        }
    }
}
