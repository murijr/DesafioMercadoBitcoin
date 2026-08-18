package com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Sentido único: negócio → apresentação. Mesma razão de existir de `BMExchange.toVM()` — é
 * aqui que a ausência vira texto (D4 de `add-exchange-detail`).
 */
fun BMExchangeDetail.toVM(resources: ResourceProvider): VMExchangeDetail {
    val locale = Locale.getDefault()
    val unavailable = resources.resolve(R.string.exchange_field_unavailable)
    val percent = NumberFormat.getPercentInstance(locale)

    return VMExchangeDetail(
        id = id,
        name = name,
        logoUrl = logoUrl,
        descriptionLabel = description ?: unavailable,
        websiteLabel = websiteUrl ?: unavailable,
        makerFeeLabel = makerFee?.let(percent::format) ?: unavailable,
        takerFeeLabel = takerFee?.let(percent::format) ?: unavailable,
        launchDateLabel =
            dateLaunched?.let { formatLaunchDate(it.atZone(ZoneOffset.UTC).toLocalDate(), locale) }
                ?: unavailable,
    )
}

/**
 * `date_launched` chega como meia-noite **UTC**; formatar no fuso do dispositivo tiraria um
 * dia de quem está a oeste de Greenwich (mesma regra de `BMExchange.toVM()`).
 */
private fun formatLaunchDate(
    date: LocalDate,
    locale: Locale,
): String =
    DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(date)
