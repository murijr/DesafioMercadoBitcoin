package com.desafiomercadobitcoin.presentation.feature.exchangelist.model

/**
 * Uma corretora como a lista a exibe.
 *
 * `volumeLabel` e `launchDateLabel` chegam aqui **já formatados**: moeda, fuso e o texto de
 * indisponibilidade são regra, e regra que mora no `Composable` fica invisível para o teste
 * de `ViewModel` (D7).
 */
data class VMExchange(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val volumeLabel: String,
    val launchDateLabel: String,
)
