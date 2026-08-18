package com.desafiomercadobitcoin.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Chaves de destino. Carregam **apenas** identificação, em forma serializável: o destino
 * busca o resto pelo seu próprio `ViewModel`, como o `app-shell` exige.
 *
 * O prefixo `VM` não se aplica aqui — chave de rota é endereço, não modelo de tela.
 */
@Serializable
data object ExchangeListKey : NavKey

@Serializable
data class ExchangeDetailKey(
    val exchangeId: Int,
) : NavKey
