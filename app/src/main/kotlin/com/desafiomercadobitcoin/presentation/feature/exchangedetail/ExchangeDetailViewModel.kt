package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper.toVM
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado do detalhe de uma *exchange* e das moedas que ela negocia.
 *
 * O `exchangeId` chega pelo `SavedStateHandle` — e não como parâmetro de construtor cru —
 * porque o G2 só permite `*UseCase`, `ResourceProvider` ou `SavedStateHandle` no construtor de
 * um `ViewModel` (D6 de `add-exchange-detail`). Quem o popula é `AppNavigation`, a partir da
 * chave de navegação.
 */
class ExchangeDetailViewModel(
    private val getExchangeDetail: GetExchangeDetailUseCase,
    private val getExchangeCurrencies: GetExchangeCurrenciesUseCase,
    private val resources: ResourceProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VMExchangeDetailState())
    val state: StateFlow<VMExchangeDetailState> = mutableState.asStateFlow()

    private val exchangeId: Int
        get() = checkNotNull(savedStateHandle.get<Int>(KEY_EXCHANGE_ID)) { "exchangeId ausente no SavedStateHandle" }

    /**
     * Popula o `SavedStateHandle` com o id recebido da chave de navegação, na primeira vez.
     * Idempotente: uma segunda chamada (recomposição) não sobrescreve o valor já presente —
     * inclusive o restaurado após morte do processo, que carrega o mesmo id (D6 de
     * `add-exchange-detail`).
     */
    fun ensureExchangeId(id: Int) {
        if (savedStateHandle.get<Int>(KEY_EXCHANGE_ID) == null) {
            savedStateHandle[KEY_EXCHANGE_ID] = id
        }
    }

    /** Única abertura de coroutine do `ViewModel`: os handlers nunca abrem a sua. */
    fun send(event: ExchangeDetailEvent) {
        viewModelScope.launch {
            when (event) {
                ExchangeDetailEvent.ScreenOpened -> handleScreenOpened()
                ExchangeDetailEvent.RetryDetailRequested -> loadDetail()
                ExchangeDetailEvent.RetryCurrenciesRequested -> loadCurrencies()
            }
        }
    }

    /**
     * As duas cargas são independentes: nenhuma espera a outra terminar para que a falha ou a
     * demora de uma não atrase a exibição da outra (D5 de `add-exchange-detail`).
     */
    private suspend fun handleScreenOpened() {
        val current = state.value
        if (current.isLoadingDetail || current.detail != null) return

        coroutineScope {
            val detailJob = async { loadDetail() }
            val currenciesJob = async { loadCurrencies() }
            detailJob.await()
            currenciesJob.await()
        }
    }

    private suspend fun loadDetail() {
        mutableState.update { it.copy(isLoadingDetail = true, detailErrorMessage = null, isDetailNotFound = false) }

        getExchangeDetail
            .execute(exchangeId)
            .onSuccess { detail ->
                mutableState.update { it.copy(detail = detail.toVM(resources), isLoadingDetail = false) }
            }.onFailure { error ->
                mutableState.update {
                    it.copy(
                        isLoadingDetail = false,
                        detailErrorMessage = messageOf(error),
                        isDetailNotFound = error == DomainError.NotFound(),
                    )
                }
            }
    }

    private suspend fun loadCurrencies() {
        mutableState.update { it.copy(isLoadingCurrencies = true, currenciesErrorMessage = null) }

        getExchangeCurrencies
            .execute(exchangeId)
            .onSuccess { currencies ->
                mutableState.update {
                    it.copy(
                        currencies =
                            currencies.map { currency ->
                                currency.toVM(resources)
                            },
                        isLoadingCurrencies = false,
                    )
                }
            }.onFailure { error ->
                mutableState.update { it.copy(isLoadingCurrencies = false, currenciesErrorMessage = messageOf(error)) }
            }
    }

    /**
     * O `UseCase` só devolve `DomainError`; o `else` existe porque a assinatura de `Result`
     * é `Throwable` e um erro sem texto seria pior do que "algo deu errado".
     */
    private fun messageOf(error: Throwable): String =
        resources.resolve((error as? DomainError ?: DomainError.Unexpected()).textKey)

    companion object {
        const val KEY_EXCHANGE_ID: String = "exchangeId"
    }
}
