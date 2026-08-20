package com.desafiomercadobitcoin.presentation.common

import android.content.Context
import android.icu.text.CompactDecimalFormat
import androidx.annotation.StringRes
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.error.TextKey
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

interface ResourceProvider {
    fun resolve(key: TextKey): String

    fun resolve(
        @StringRes id: Int,
    ): String

    fun formatUsd(value: Double): String

    fun formatLaunchDate(date: Instant): String
}

class AndroidResourceProvider(
    private val context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val decimalFormat: CompactDecimalFormat =
        CompactDecimalFormat.getInstance(
            locale,
            CompactDecimalFormat.CompactStyle.SHORT,
        ),
) : ResourceProvider {
    override fun resolve(key: TextKey): String = context.getString(key.toStringRes())

    override fun resolve(
        @StringRes id: Int,
    ): String = context.getString(id)

    override fun formatUsd(value: Double): String =
        String.format(
            locale,
            resolve(R.string.exchange_volume_format),
            Currency.getInstance(USD).getSymbol(locale),
            decimalFormat.format(value),
        )

    override fun formatLaunchDate(date: Instant): String =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(date.atZone(ZoneOffset.UTC).toLocalDate())

    private companion object {
        const val USD = "USD"
    }
}

@StringRes
internal fun TextKey.toStringRes(): Int =
    when (this) {
        TextKey.InvalidInput -> R.string.error_invalid_input
        TextKey.NotFound -> R.string.error_not_found
        TextKey.NetworkUnavailable -> R.string.error_network_unavailable
        TextKey.UnexpectedResponse -> R.string.error_unexpected_response
        TextKey.Unexpected -> R.string.error_unexpected
    }
