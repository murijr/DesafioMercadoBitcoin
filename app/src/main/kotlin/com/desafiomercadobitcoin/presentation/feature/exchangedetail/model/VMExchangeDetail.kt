package com.desafiomercadobitcoin.presentation.feature.exchangedetail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VMExchangeDetail(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val descriptionLabel: String,
    val websiteLabel: String,
    val makerFeeLabel: String,
    val takerFeeLabel: String,
    val launchDateLabel: String,
) : Parcelable
