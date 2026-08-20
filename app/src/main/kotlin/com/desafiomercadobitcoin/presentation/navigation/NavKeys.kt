package com.desafiomercadobitcoin.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ExchangeListKey : NavKey

@Serializable
data class ExchangeDetailKey(
    val exchangeId: Int,
) : NavKey
