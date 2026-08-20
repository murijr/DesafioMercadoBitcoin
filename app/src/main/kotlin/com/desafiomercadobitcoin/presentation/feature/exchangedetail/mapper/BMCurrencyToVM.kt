package com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import java.text.NumberFormat
import java.util.Locale

fun BMCurrency.toVM(resources: ResourceProvider): VMCurrency =
    VMCurrency(
        name = name,
        priceLabel =
            priceUsd?.let { NumberFormat.getCurrencyInstance(Locale.US).format(it) }
                ?: resources.resolve(R.string.exchange_field_unavailable),
    )
