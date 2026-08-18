package com.desafiomercadobitcoin.presentation.feature.exchangelist.mapper

import android.icu.text.CompactDecimalFormat
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/**
 * Sentido único: negócio → apresentação. O nome é `toVM()` e não `to()` porque o Detekt
 * exige nome de função com ao menos três caracteres (ver `app/AGENTS.md`).
 *
 * É aqui que a ausência vira texto: o domínio descreve o fato, a apresentação o comunica.
 */
fun BMExchange.toVM(resources: ResourceProvider): VMExchange {
    val locale = Locale.getDefault()
    val unavailable = resources.resolve(R.string.exchange_field_unavailable)

    return VMExchange(
        id = id,
        name = name,
        logoUrl = logoUrl,
        volumeLabel = spotVolumeUsd?.let { resources.formatUsd(it, locale) } ?: unavailable,
        launchDateLabel =
            dateLaunched?.let { formatLaunchDate(it.atZone(ZoneOffset.UTC).toLocalDate(), locale) }
                ?: unavailable,
    )
}

/**
 * Compacto porque o valor integral ocuparia a linha inteira do item (D7). O símbolo vem do
 * `Currency`, e a ordem entre símbolo e número vem do recurso — há localidades que a invertem.
 */
private fun ResourceProvider.formatUsd(
    value: Double,
    locale: Locale,
): String {
    val amount =
        CompactDecimalFormat
            .getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
            .format(value)

    return String.format(
        locale,
        resolve(R.string.exchange_volume_format),
        Currency.getInstance(USD).getSymbol(locale),
        amount,
    )
}

/**
 * `date_launched` chega como meia-noite **UTC**; formatar no fuso do dispositivo tiraria um
 * dia de quem está a oeste de Greenwich (D3).
 */
private fun formatLaunchDate(
    date: LocalDate,
    locale: Locale,
): String =
    DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(date)

private const val USD = "USD"
