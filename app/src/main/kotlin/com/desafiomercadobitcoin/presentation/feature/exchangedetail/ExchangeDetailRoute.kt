package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExchangeDetailRoute(
    exchangeId: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExchangeDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.send(ExchangeDetailEvent.ScreenOpened(exchangeId))
    }

    ExchangeDetailScreen(state = state, onEvent = viewModel::send, onBackClick = onBackClick, modifier = modifier)
}
