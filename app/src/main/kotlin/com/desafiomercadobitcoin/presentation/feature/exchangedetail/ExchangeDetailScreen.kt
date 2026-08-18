package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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

/**
 * Tela sem estado próprio: recebe o que renderizar e devolve o que o usuário fez — mesma
 * razão de `ExchangeListScreen`.
 *
 * O detalhe e a listagem de moedas têm estados de carregamento, erro e nova tentativa
 * independentes (D5/D7 de `add-exchange-detail`): a listagem só aparece depois que o detalhe
 * é obtido com sucesso, mas sua própria falha não afeta o detalhe já exibido.
 *
 * `onBackClick` é a ação do botão de voltar da `TopAppBar` — a tela não sabe *como* voltar
 * (isso é da pilha de navegação, em `AppNavigation`), só emite a intenção. O retorno pelo
 * sistema (gesto/botão) continua funcionando à parte, via `NavDisplay.onBack` (`app-shell`).
 *
 * `Scaffold` com `TopAppBar` herda `contentWindowInsets` (`WindowInsets.safeDrawing` por
 * padrão) — o app é edge-to-edge (`enableEdgeToEdge()` na `MainActivity`), e sem isso o
 * logotipo do cabeçalho renderiza atrás da barra de status, mesma razão de `ExchangeListScreen`.
 */
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
        else ->
            items(items = state.currencies, key = { it.name }) { currency ->
                CurrencyListItem(currency = currency, modifier = Modifier.testTag(TAG_CURRENCIES_LIST))
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
