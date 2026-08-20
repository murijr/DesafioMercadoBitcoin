package com.desafiomercadobitcoin.presentation.feature.exchangedetail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VMCurrency(
    val name: String,
    val priceLabel: String,
) : Parcelable
