package com.desafiomercadobitcoin.presentation.feature.exchangedetail.components

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `ExchangeDetailHeader` isolado, sem montar `ExchangeDetailScreen` (G9): confirma que o
 * componente renderiza seus campos, e degrada cada campo opcional ausente individualmente, em
 * dispositivo/emulador real.
 */
@RunWith(AndroidJUnit4::class)
class ExchangeDetailHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val unavailable get() = context.getString(R.string.exchange_field_unavailable)

    @Test
    fun givenAllFieldsWhenRenderingThenEveryFieldIsDisplayed() {
        render(
            detail(
                descriptionLabel = "Uma exchange",
                websiteLabel = "https://exchange.example",
                makerFeeLabel = "2%",
                takerFeeLabel = "4%",
                launchDateLabel = "14 de jul. de 2017",
            ),
        )

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                context.getString(R.string.exchange_detail_id_format, EXCHANGE_ID),
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Uma exchange").assertIsDisplayed()
        composeRule.onNodeWithText("https://exchange.example").assertIsDisplayed()
        composeRule.onNodeWithText("2%").assertIsDisplayed()
        composeRule.onNodeWithText("4%").assertIsDisplayed()
        composeRule.onNodeWithText("14 de jul. de 2017").assertIsDisplayed()
    }

    @Test
    fun givenDescriptionUnavailableWhenRenderingThenTheUnavailableTextIsDisplayed() {
        render(detail(descriptionLabel = unavailable))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenWebsiteUnavailableWhenRenderingThenTheUnavailableTextIsDisplayed() {
        render(detail(websiteLabel = unavailable))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenLaunchDateUnavailableWhenRenderingThenTheUnavailableTextIsDisplayed() {
        render(detail(launchDateLabel = unavailable))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
        composeRule.onNodeWithText(unavailable).assertIsDisplayed()
    }

    @Test
    fun givenNoLogoWhenRenderingThenTheOtherFieldsAreUnaffected() {
        render(detail(logoUrl = null))

        composeRule.onNodeWithText("Binance").assertIsDisplayed()
    }

    private fun render(detail: VMExchangeDetail) {
        composeRule.setContent {
            AppTheme {
                ExchangeDetailHeader(detail = detail)
            }
        }
    }

    private fun detail(
        logoUrl: String? = null,
        descriptionLabel: String = "Uma exchange",
        websiteLabel: String = "https://exchange.example",
        makerFeeLabel: String = "2%",
        takerFeeLabel: String = "4%",
        launchDateLabel: String = "14 de jul. de 2017",
    ) = VMExchangeDetail(
        id = EXCHANGE_ID,
        name = "Binance",
        logoUrl = logoUrl,
        descriptionLabel = descriptionLabel,
        websiteLabel = websiteLabel,
        makerFeeLabel = makerFeeLabel,
        takerFeeLabel = takerFeeLabel,
        launchDateLabel = launchDateLabel,
    )
}

private const val EXCHANGE_ID = 270
