package com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import java.text.NumberFormat
import java.util.Locale

/**
 * Preço integral, e não compacto: ao contrário do volume da listagem, o preço unitário de uma
 * moeda precisa da precisão inteira, sob pena de confundir "US$ 0" com indisponível em moedas
 * de preço fracionário pequeno (D8 de `add-exchange-detail`).
 */
fun BMCurrency.toVM(resources: ResourceProvider): VMCurrency =
    VMCurrency(
        name = name,
        priceLabel =
            priceUsd?.let { NumberFormat.getCurrencyInstance(Locale.US).format(it) }
                ?: resources.resolve(R.string.exchange_field_unavailable),
    )
