package com.desafiomercadobitcoin.presentation.feature.exchangelist.model

data class VMExchange(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val volumeLabel: String,
    val launchDateLabel: String,
)
