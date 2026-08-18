package com.desafiomercadobitcoin.presentation.feature.exchangelist

import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange

/**
 * O que a tela deve renderizar agora.
 *
 * A falha de um lote posterior é um **campo** deste estado, e não a sua substituição por um
 * estado de erro: o `spec` proíbe descartar o que já está exibido (D8).
 */
data class VMExchangeListState(
    val items: List<VMExchange> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val pagingErrorMessage: String? = null,
) {
    /** Sucesso sem nenhuma corretora ativa — distinto de falha e de carregamento. */
    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading && errorMessage == null
}
