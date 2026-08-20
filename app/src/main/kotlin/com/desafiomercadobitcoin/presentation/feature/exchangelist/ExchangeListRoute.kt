package com.desafiomercadobitcoin.presentation.feature.exchangelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

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
