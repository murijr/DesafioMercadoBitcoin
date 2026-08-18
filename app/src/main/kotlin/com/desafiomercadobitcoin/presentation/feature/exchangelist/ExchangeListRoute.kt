package com.desafiomercadobitcoin.presentation.feature.exchangelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Liga a tela ao seu `ViewModel`. O destino de detalhe é aberto por efeito, e não por
 * estado, porque navegar é acontecimento de uma vez só.
 */
@Composable
fun ExchangeListRoute(
    onExchangeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExchangeListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.send(ExchangeListEvent.ScreenOpened)
    }

    LaunchedEffect(viewModel, onExchangeSelected) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ExchangeListEffect.OpenExchangeDetail -> onExchangeSelected(effect.exchangeId)
            }
        }
    }

    ExchangeListScreen(state = state, onEvent = viewModel::send, modifier = modifier)
}
