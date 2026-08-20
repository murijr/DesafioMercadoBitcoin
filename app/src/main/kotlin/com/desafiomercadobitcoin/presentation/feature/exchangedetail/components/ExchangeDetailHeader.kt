package com.desafiomercadobitcoin.presentation.feature.exchangedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.model.VMExchangeDetail
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ExchangeDetailHeader(
    detail: VMExchangeDetail,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AsyncImage(
            model = detail.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(64.dp).clip(CircleShape),
        )
        Text(text = detail.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.exchange_detail_id_format, detail.id),
            style = MaterialTheme.typography.bodySmall,
        )
        Markdown(content = detail.descriptionLabel, modifier = Modifier.fillMaxWidth())
        Text(text = detail.websiteLabel, style = MaterialTheme.typography.bodyMedium)
        Text(text = detail.makerFeeLabel, style = MaterialTheme.typography.bodyMedium)
        Text(text = detail.takerFeeLabel, style = MaterialTheme.typography.bodyMedium)
        Text(text = detail.launchDateLabel, style = MaterialTheme.typography.bodySmall)
    }
}
