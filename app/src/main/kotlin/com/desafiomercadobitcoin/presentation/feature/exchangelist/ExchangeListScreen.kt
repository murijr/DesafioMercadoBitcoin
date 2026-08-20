package com.desafiomercadobitcoin.presentation.feature.exchangelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangelist.components.ExchangeListItem

const val TAG_INITIAL_LOADING: String = "exchangeListInitialLoading"
const val TAG_PAGING_LOADING: String = "exchangeListPagingLoading"
const val TAG_LIST: String = "exchangeList"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeListScreen(
    state: VMExchangeListState,
    onEvent: (ExchangeListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.exchange_list_title)) }) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading && state.items.isEmpty() -> InitialLoading()
                state.errorMessage != null -> ErrorState(message = state.errorMessage, onEvent = onEvent)
                state.isEmpty -> EmptyState()
                else -> ExchangeContent(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ExchangeContent(
    state: VMExchangeListState,
    onEvent: (ExchangeListEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    NotifyWhenNearTheEnd(listState = listState, enabled = state.hasMore, onEvent = onEvent)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().testTag(TAG_LIST),
    ) {
        items(items = state.items, key = { it.id }) { exchange ->
            ExchangeListItem(
                exchange = exchange,
                onClick = { onEvent(ExchangeListEvent.ExchangeSelected(exchange.id)) },
            )
        }
        item {
            ListFooter(
                isLoadingMore = state.isLoadingMore,
                pagingErrorMessage = state.pagingErrorMessage,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun NotifyWhenNearTheEnd(
    listState: LazyListState,
    enabled: Boolean,
    onEvent: (ExchangeListEvent) -> Unit,
) {
    val isNearTheEnd by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= layout.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(isNearTheEnd, enabled) {
        if (isNearTheEnd && enabled) onEvent(ExchangeListEvent.NextPageRequested)
    }
}

@Composable
private fun ListFooter(
    isLoadingMore: Boolean,
    pagingErrorMessage: String?,
    onEvent: (ExchangeListEvent) -> Unit,
) {
    when {
        isLoadingMore ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(modifier = Modifier.testTag(TAG_PAGING_LOADING)) }

        pagingErrorMessage != null ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = pagingErrorMessage, style = MaterialTheme.typography.bodyMedium)
                RetryButton(onEvent = onEvent)
            }
    }
}

@Composable
private fun InitialLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.testTag(TAG_INITIAL_LOADING))
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.exchange_list_empty),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onEvent: (ExchangeListEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        RetryButton(onEvent = onEvent)
    }
}

@Composable
private fun RetryButton(onEvent: (ExchangeListEvent) -> Unit) {
    TextButton(onClick = { onEvent(ExchangeListEvent.RetryRequested) }) {
        Text(text = stringResource(R.string.exchange_list_retry))
    }
}

private const val LOAD_MORE_THRESHOLD = 5
