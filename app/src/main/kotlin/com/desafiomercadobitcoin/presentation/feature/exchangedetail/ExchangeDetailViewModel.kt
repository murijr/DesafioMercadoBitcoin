package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.mapper.toVM
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExchangeDetailViewModel(
    private val getExchangeDetail: GetExchangeDetailUseCase,
    private val getExchangeCurrencies: GetExchangeCurrenciesUseCase,
    private val resources: ResourceProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableState = savedStateHandle.getMutableStateFlow(KEY_EXCHANGE_DETAIL, VMExchangeDetailState())
    val state: StateFlow<VMExchangeDetailState> = mutableState.asStateFlow()

    fun send(event: ExchangeDetailEvent) {
        viewModelScope.launch {
            when (event) {
                is ExchangeDetailEvent.ScreenOpened -> handleScreenOpened(event.exchangeId)
                ExchangeDetailEvent.RetryDetailRequested -> loadDetail(requireExchangeId())
                ExchangeDetailEvent.RetryCurrenciesRequested -> loadCurrencies(requireExchangeId())
            }
        }
    }

    private suspend fun handleScreenOpened(exchangeId: Int) {
        val current = state.value
        if (current.exchangeId == exchangeId && (current.isLoadingDetail || current.detail != null)) return

        mutableState.update { it.copy(exchangeId = exchangeId) }

        coroutineScope {
            launch { loadDetail(exchangeId) }
            launch { loadCurrencies(exchangeId) }
        }
    }

    private suspend fun loadDetail(exchangeId: Int) {
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

    private suspend fun loadCurrencies(exchangeId: Int) {
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

    private fun requireExchangeId(): Int = checkNotNull(state.value.exchangeId) { "exchangeId ausente no state" }

    private fun messageOf(error: Throwable): String =
        resources.resolve((error as? DomainError ?: DomainError.Unexpected()).textKey)

    companion object {
        private const val KEY_EXCHANGE_DETAIL: String = "exchange_detail"
    }
}
