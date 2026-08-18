package com.desafiomercadobitcoin.presentation.feature.exchangedetail.model

/**
 * Uma *exchange* como a tela de detalhe a exibe. Todo *label* chega já formatado — moeda,
 * percentual, fuso e o texto de indisponibilidade são regra da apresentação, e regra que mora
 * no `Composable` fica invisível para o teste de `ViewModel` (mesma razão de `VMExchange`).
 */
data class VMExchangeDetail(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val descriptionLabel: String,
    val websiteLabel: String,
    val makerFeeLabel: String,
    val takerFeeLabel: String,
    val launchDateLabel: String,
)
