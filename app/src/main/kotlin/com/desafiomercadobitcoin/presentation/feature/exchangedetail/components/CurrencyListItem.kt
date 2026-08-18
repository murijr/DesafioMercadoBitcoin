package com.desafiomercadobitcoin.presentation.feature.exchangedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMCurrency

@Composable
fun CurrencyListItem(
    currency: VMCurrency,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = currency.name, style = MaterialTheme.typography.bodyLarge)
        Text(text = currency.priceLabel, style = MaterialTheme.typography.bodyMedium)
    }
}
