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

    fun ensureExchangeId(id: Int) {
        if (savedStateHandle.get<Int>(KEY_EXCHANGE_ID) == null) {
            savedStateHandle[KEY_EXCHANGE_ID] = id
        }
    }

    fun send(event: ExchangeDetailEvent) {
        viewModelScope.launch {
            when (event) {
                ExchangeDetailEvent.ScreenOpened -> handleScreenOpened()
                ExchangeDetailEvent.RetryDetailRequested -> loadDetail()
                ExchangeDetailEvent.RetryCurrenciesRequested -> loadCurrencies()
            }
        }
    }

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

    private fun messageOf(error: Throwable): String =
        resources.resolve((error as? DomainError ?: DomainError.Unexpected()).textKey)

    companion object {
        const val KEY_EXCHANGE_ID: String = "exchangeId"
    }
}
