package com.desafiomercadobitcoin.presentation.feature.exchangelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangelist.mapper.toVM
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da listagem de corretoras.
 *
 * O `SavedStateHandle` guarda a **página alcançada**, e não os itens: restaurar centenas de
 * objetos por `Bundle` estouraria o limite da transação (D8). A mudança de configuração já é
 * coberta pelo próprio `ViewModel`.
 */
class ExchangeListViewModel(
    private val getExchangePage: GetExchangePageUseCase,
    private val resources: ResourceProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VMExchangeListState())
    val state: StateFlow<VMExchangeListState> = mutableState.asStateFlow()

    // Sem `replay`: um efeito de navegacao reentregue a um novo coletor navegaria de novo.
    private val mutableEffect = MutableSharedFlow<ExchangeListEffect>(extraBufferCapacity = 1)
    val effect: SharedFlow<ExchangeListEffect> = mutableEffect.asSharedFlow()

    /** Única abertura de coroutine do `ViewModel`: os handlers nunca abrem a sua. */
    fun send(event: ExchangeListEvent) {
        viewModelScope.launch {
            when (event) {
                ExchangeListEvent.ScreenOpened -> handleScreenOpened()
                ExchangeListEvent.NextPageRequested -> loadNextPage()
                ExchangeListEvent.RetryRequested -> handleRetry()
                is ExchangeListEvent.ExchangeSelected ->
                    mutableEffect.emit(ExchangeListEffect.OpenExchangeDetail(event.exchangeId))
            }
        }
    }

    private suspend fun handleScreenOpened() {
        val current = state.value
        if (current.isLoading || current.items.isNotEmpty()) return

        // Lido antes da carga: publicar a primeira pagina ja reescreve a pagina alcancada.
        val reached = savedStateHandle.get<Int>(KEY_REACHED_PAGE) ?: FIRST_PAGE

        loadFirstPage()
        while (state.value.page < reached && canLoadMore()) {
            loadNextPage()
        }
    }

    private suspend fun handleRetry() {
        if (state.value.errorMessage != null) loadFirstPage() else loadNextPage()
    }

    private suspend fun loadFirstPage() {
        mutableState.update {
            it.copy(isLoading = true, errorMessage = null, pagingErrorMessage = null)
        }
        getExchangePage
            .execute(FIRST_PAGE)
            .onSuccess { page -> publishFirst(page) }
            .onFailure { error ->
                mutableState.update {
                    it.copy(isLoading = false, items = emptyList(), errorMessage = messageOf(error))
                }
            }
    }

    private suspend fun loadNextPage() {
        if (!canLoadMore()) return
        mutableState.update { it.copy(isLoadingMore = true, pagingErrorMessage = null) }

        getExchangePage
            .execute(state.value.page + 1)
            .onSuccess { page -> publishNext(page) }
            .onFailure { error ->
                mutableState.update {
                    it.copy(isLoadingMore = false, pagingErrorMessage = messageOf(error))
                }
            }
    }

    /**
     * Lido **antes** da primeira suspensão de [loadNextPage]: é o que impede a rolagem de
     * emitir uma segunda solicitação para o lote que já está em andamento.
     */
    private fun canLoadMore(): Boolean = with(state.value) { hasMore && !isLoading && !isLoadingMore }

    private fun publishFirst(page: BMExchangePage) {
        mutableState.update {
            it.copy(
                items = page.items.map { item -> item.toVM(resources) },
                page = page.page,
                hasMore = page.hasMore,
                isLoading = false,
                errorMessage = null,
            )
        }
        savedStateHandle[KEY_REACHED_PAGE] = page.page
    }

    private fun publishNext(page: BMExchangePage) {
        mutableState.update {
            it.copy(
                items = it.items + page.items.map { item -> item.toVM(resources) },
                page = page.page,
                hasMore = page.hasMore,
                isLoadingMore = false,
            )
        }
        savedStateHandle[KEY_REACHED_PAGE] = page.page
    }

    /**
     * O `UseCase` só devolve `DomainError`; o `else` existe porque a assinatura de `Result`
     * é `Throwable` e um erro sem texto seria pior do que "algo deu errado".
     */
    private fun messageOf(error: Throwable): String =
        resources.resolve((error as? DomainError ?: DomainError.Unexpected).textKey)

    companion object {
        const val KEY_REACHED_PAGE: String = "reachedPage"
        private const val FIRST_PAGE = 0
    }
}
