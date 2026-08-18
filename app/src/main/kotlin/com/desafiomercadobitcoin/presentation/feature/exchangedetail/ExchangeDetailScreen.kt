package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.desafiomercadobitcoin.R

/**
 * Destino mínimo: exibe só a identificação que recebeu. O conteúdo real — descrição, *urls*,
 * taxas, países — é a mudança seguinte; aqui ele existe para provar que a pilha funciona.
 */
@Composable
fun ExchangeDetailScreen(
    exchangeId: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.exchange_detail_title, exchangeId),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
