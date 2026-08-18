package com.desafiomercadobitcoin.presentation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.theme.AppTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

/**
 * Prova que UI Compose roda na JVM com sombra Android, sem emulador (G7).
 */
@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given the home screen when it is rendered then the title is displayed`() {
        composeRule.setContent {
            AppTheme { HomeScreen() }
        }

        composeRule.onNodeWithText(context.getString(R.string.home_title)).assertExists()
    }

    @Test
    fun `given dark mode when the home screen is rendered then it still displays its content`() {
        composeRule.setContent {
            AppTheme(useDarkTheme = true) { HomeScreen() }
        }

        composeRule.onNodeWithText(context.getString(R.string.home_placeholder)).assertExists()
    }
}
