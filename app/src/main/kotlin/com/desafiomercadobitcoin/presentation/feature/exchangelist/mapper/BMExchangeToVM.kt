package com.desafiomercadobitcoin.presentation.feature.exchangelist.mapper

import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange

fun BMExchange.toVM(resources: ResourceProvider): VMExchange {
    val unavailable = resources.resolve(R.string.exchange_field_unavailable)

    return VMExchange(
        id = id,
        name = name,
        logoUrl = logoUrl,
        volumeLabel = spotVolumeUsd?.let { resources.formatUsd(it) } ?: unavailable,
        launchDateLabel =
            dateLaunched?.let { resources.formatLaunchDate(it) }
                ?: unavailable,
    )
}
