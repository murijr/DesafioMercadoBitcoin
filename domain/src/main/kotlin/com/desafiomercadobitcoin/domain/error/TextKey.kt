package com.desafiomercadobitcoin.domain.error

sealed interface TextKey {
    data object InvalidInput : TextKey

    data object NotFound : TextKey

    data object NetworkUnavailable : TextKey

    data object UnexpectedResponse : TextKey

    data object Unexpected : TextKey
}
