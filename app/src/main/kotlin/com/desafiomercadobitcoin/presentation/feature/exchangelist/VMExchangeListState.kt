package com.desafiomercadobitcoin.presentation.feature.exchangelist

import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange

data class VMExchangeListState(
    val items: List<VMExchange> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val pagingErrorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading && errorMessage == null
}
