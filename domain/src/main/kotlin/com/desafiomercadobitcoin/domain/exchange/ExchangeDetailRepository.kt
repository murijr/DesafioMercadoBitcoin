package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

/**
 * Contrato de leitura do detalhe de uma *exchange* e das moedas que ela negocia.
 *
 * Os dois métodos servem a mesma tela e o mesmo `exchangeId`, sem estado a manter — ao
 * contrário de [ExchangeRepository], não há razão para dividir em dois contratos (D3 de
 * `add-exchange-detail`).
 */
interface ExchangeDetailRepository {
    suspend fun loadDetail(exchangeId: Int): BMExchangeDetail

    suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency>
}
