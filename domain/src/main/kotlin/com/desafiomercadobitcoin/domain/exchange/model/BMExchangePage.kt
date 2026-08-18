package com.desafiomercadobitcoin.domain.exchange.model

/**
 * Um lote do catálogo. `hasMore` responde "ainda restam corretoras no índice?" sem que o
 * chamador precise conhecer o tamanho do lote nem o total — isso é detalhe de transporte.
 */
data class BMExchangePage(
    val items: List<BMExchange>,
    val page: Int,
    val hasMore: Boolean,
)
