package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail

/**
 * O que a tela deve renderizar agora.
 *
 * O detalhe e a listagem de moedas carregam campos independentes de carregamento e de erro —
 * a falha de um não substitui nem descarta o conteúdo do outro (D5 de `add-exchange-detail`).
 */
data class VMExchangeDetailState(
    val detail: VMExchangeDetail? = null,
    val isLoadingDetail: Boolean = false,
    val detailErrorMessage: String? = null,
    val isDetailNotFound: Boolean = false,
    val currencies: List<VMCurrency> = emptyList(),
    val isLoadingCurrencies: Boolean = false,
    val currenciesErrorMessage: String? = null,
) {
    /** Sucesso sem nenhuma moeda negociada — distinto de falha e de carregamento. */
    val isCurrenciesEmpty: Boolean
        get() = currencies.isEmpty() && !isLoadingCurrencies && currenciesErrorMessage == null && detail != null
}
