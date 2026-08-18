package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage

/**
 * Contrato de leitura do catálogo de corretoras.
 *
 * A paginação é do domínio; **como** a página é montada — um índice memoizado mais uma
 * consulta de conteúdo por lote — é detalhe de `:data` (D1).
 */
interface ExchangeRepository {
    suspend fun loadPage(page: Int): BMExchangePage
}
