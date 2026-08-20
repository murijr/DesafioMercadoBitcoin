package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.components.CurrencyListItem
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.components.ExchangeDetailHeader

const val TAG_DETAIL_LOADING: String = "exchangeDetailLoading"
const val TAG_DETAIL_ERROR: String = "exchangeDetailError"
const val TAG_DETAIL_NOT_FOUND: String = "exchangeDetailNotFound"
const val TAG_DETAIL_RETRY: String = "exchangeDetailRetry"
const val TAG_CURRENCIES_LOADING: String = "exchangeCurrenciesLoading"
const val TAG_CURRENCIES_ERROR: String = "exchangeCurrenciesError"
const val TAG_CURRENCIES_EMPTY: String = "exchangeCurrenciesEmpty"
const val TAG_CURRENCIES_RETRY: String = "exchangeCurrenciesRetry"
const val TAG_CURRENCIES_LIST: String = "exchangeCurrenciesList"
const val TAG_DETAIL_CONTENT: String = "exchangeDetailContent"
const val TAG_DETAIL_BACK: String = "exchangeDetailBack"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeDetailScreen(
    state: VMExchangeDetailState,
    onEvent: (ExchangeDetailEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag(TAG_DETAIL_BACK)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exchange_detail_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isDetailNotFound -> NotFoundState(message = requireNotNull(state.detailErrorMessage))
                state.detail == null && state.detailErrorMessage != null ->
                    DetailErrorState(message = state.detailErrorMessage, onEvent = onEvent)
                state.detail == null && state.isLoadingDetail -> InitialLoading()
                state.detail != null -> DetailContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DetailContent(
    state: VMExchangeDetailState,
    onEvent: (ExchangeDetailEvent) -> Unit,
) {
    val detail = requireNotNull(state.detail)
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(TAG_DETAIL_CONTENT)) {
        item { ExchangeDetailHeader(detail = detail) }
        item {
            Text(
                text = stringResource(R.string.exchange_detail_currencies_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        currenciesSection(state = state, onEvent = onEvent)
    }
}

private fun LazyListScope.currenciesSection(
    state: VMExchangeDetailState,
    onEvent: (ExchangeDetailEvent) -> Unit,
) {
    when {
        state.isLoadingCurrencies && state.currencies.isEmpty() -> item { CurrenciesLoading() }
        state.currenciesErrorMessage != null ->
            item { CurrenciesError(message = state.currenciesErrorMessage, onEvent = onEvent) }
        state.isCurrenciesEmpty -> item { CurrenciesEmpty() }
        else -> {
            itemsIndexed(
                items = state.currencies,
                key = { index, currency -> "$index-${currency.name}" },
            ) { _, currency ->
                CurrencyListItem(
                    currency = currency,
                    modifier = Modifier.testTag(TAG_CURRENCIES_LIST),
                )
            }
        }
    }
}

@Composable
private fun CurrenciesLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(modifier = Modifier.testTag(TAG_CURRENCIES_LOADING)) }
}

@Composable
private fun CurrenciesError(
    message: String,
    onEvent: (ExchangeDetailEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag(TAG_CURRENCIES_ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        TextButton(
            onClick = { onEvent(ExchangeDetailEvent.RetryCurrenciesRequested) },
            modifier = Modifier.testTag(TAG_CURRENCIES_RETRY),
        ) { Text(text = stringResource(R.string.exchange_list_retry)) }
    }
}

@Composable
private fun CurrenciesEmpty() {
    Text(
        text = stringResource(R.string.exchange_detail_currencies_empty),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(16.dp).testTag(TAG_CURRENCIES_EMPTY),
    )
}

@Composable
private fun InitialLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.testTag(TAG_DETAIL_LOADING))
    }
}

@Composable
private fun DetailErrorState(
    message: String,
    onEvent: (ExchangeDetailEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag(TAG_DETAIL_ERROR),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        TextButton(
            onClick = { onEvent(ExchangeDetailEvent.RetryDetailRequested) },
            modifier = Modifier.testTag(TAG_DETAIL_RETRY),
        ) { Text(text = stringResource(R.string.exchange_list_retry)) }
    }
}

@Composable
private fun NotFoundState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag(TAG_DETAIL_NOT_FOUND),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
