package com.desafiomercadobitcoin.presentation.common

import android.content.Context
import androidx.annotation.StringRes
import com.desafiomercadobitcoin.R
import com.desafiomercadobitcoin.domain.error.TextKey

/**
 * Única ponte entre as chaves do domínio e os recursos de texto do aplicativo.
 *
 * `:domain` e `:data` nunca veem `Context`: quem traduz é a apresentação, antes de
 * publicar `State.Error` ou `Effect`.
 */
interface ResourceProvider {
    fun resolve(key: TextKey): String
}

class AndroidResourceProvider(
    private val context: Context,
) : ResourceProvider {
    override fun resolve(key: TextKey): String = context.getString(key.toStringRes())
}

/**
 * `when` exaustivo de propósito: uma [TextKey] nova deixa de compilar até ganhar tradução.
 */
@StringRes
internal fun TextKey.toStringRes(): Int =
    when (this) {
        TextKey.InvalidInput -> R.string.error_invalid_input
        TextKey.NotFound -> R.string.error_not_found
        TextKey.NetworkUnavailable -> R.string.error_network_unavailable
        TextKey.UnexpectedResponse -> R.string.error_unexpected_response
        TextKey.Unexpected -> R.string.error_unexpected
    }
