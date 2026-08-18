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

    /**
     * Texto que nasce na apresentacao e nao tem chave de dominio — rotulo de campo
     * indisponivel, molde de formatacao. O `ViewModel` precisa dele para publicar texto
     * ja resolvido, como o `app-shell` exige.
     */
    fun resolve(
        @StringRes id: Int,
    ): String
}

class AndroidResourceProvider(
    private val context: Context,
) : ResourceProvider {
    override fun resolve(key: TextKey): String = context.getString(key.toStringRes())

    override fun resolve(
        @StringRes id: Int,
    ): String = context.getString(id)
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
