package com.desafiomercadobitcoin.presentation.feature.exchangelist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.desafiomercadobitcoin.presentation.feature.exchangelist.model.VMExchange

@Composable
fun ExchangeListItem(
    exchange: VMExchange,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = exchange.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = exchange.name, style = MaterialTheme.typography.titleMedium)
            Text(text = exchange.volumeLabel, style = MaterialTheme.typography.bodyMedium)
            Text(text = exchange.launchDateLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
