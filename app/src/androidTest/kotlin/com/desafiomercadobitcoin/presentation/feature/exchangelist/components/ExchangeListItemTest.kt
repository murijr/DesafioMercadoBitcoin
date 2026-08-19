package com.desafiomercadobitcoin.presentation.feature.exchangelist.components

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `ExchangeListItem` isolado, sem montar `ExchangeListScreen` (G9): confirma que o componente
 * renderiza seus dados e reage ao toque por conta própria, em dispositivo/emulador real.
 */
@RunWith(AndroidJUnit4::class)
class ExchangeListItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun givenAllFieldsWhenRenderingThenEveryFieldIsDisplayed() {
        render(exchange(name = "Binance", volumeLabel = "US$ 1,2 bi", launchDateLabel = "14 de jul. de 2017"))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText("US$ 1,2 bi").assertIsDisplayed()
        composeRule.onNodeWithText("14 de jul. de 2017").assertIsDisplayed()
    }

    @Test
    fun givenVolumeUnavailableWhenRenderingThenTheUnavailableTextIsDisplayed() {
        val unavailable = context.getString(R.string.exchange_field_unavailable)
        render(exchange(name = "Binance", volumeLabel = unavailable))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenLaunchDateUnavailableWhenRenderingThenTheUnavailableTextIsDisplayed() {
        val unavailable = context.getString(R.string.exchange_field_unavailable)
        render(exchange(name = "Binance", launchDateLabel = unavailable))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenNoLogoWhenRenderingThenTheOtherFieldsAreUnaffected() {
        render(exchange(name = "Binance", logoUrl = null))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
    }

    @Test
    fun givenTheItemWhenTappedThenTheClickCallbackFiresExactlyOnce() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                ExchangeListItem(exchange = exchange(name = "Binance"), onClick = { clicks++ })
            }
        }

        composeRule.onNodeWithText("Binance").performClick()

        assertEquals(1, clicks)
    }

    private fun render(exchange: VMExchange) {
        composeRule.setContent {
            AppTheme {
                ExchangeListItem(exchange = exchange, onClick = {})
            }
        }
    }

    private fun exchange(
        name: String,
        logoUrl: String? = null,
        volumeLabel: String = "US$ 1,2 bi",
        launchDateLabel: String = "14 de jul. de 2017",
    ) = VMExchange(
        id = 1,
        name = name,
        logoUrl = logoUrl,
        volumeLabel = volumeLabel,
        launchDateLabel = launchDateLabel,
    )
}
