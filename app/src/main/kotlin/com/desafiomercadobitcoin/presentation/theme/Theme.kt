package com.desafiomercadobitcoin.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = BrandGreen,
        onPrimary = OnBrand,
        secondary = BrandGreenDark,
        background = NeutralLight,
        surface = NeutralLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = BrandGreenLight,
        onPrimary = NeutralDark,
        secondary = BrandGreen,
        background = NeutralDark,
        surface = NeutralDark,
    )

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
