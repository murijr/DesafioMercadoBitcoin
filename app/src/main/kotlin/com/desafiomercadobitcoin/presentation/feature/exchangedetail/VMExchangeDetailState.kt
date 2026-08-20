package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail

data class VMExchangeDetailState(
    val detail: VMExchangeDetail? = null,
    val isLoadingDetail: Boolean = false,
    val detailErrorMessage: String? = null,
    val isDetailNotFound: Boolean = false,
    val currencies: List<VMCurrency> = emptyList(),
    val isLoadingCurrencies: Boolean = false,
    val currenciesErrorMessage: String? = null,
) {
    val isCurrenciesEmpty: Boolean
        get() = currencies.isEmpty() && !isLoadingCurrencies && currenciesErrorMessage == null && detail != null
}
