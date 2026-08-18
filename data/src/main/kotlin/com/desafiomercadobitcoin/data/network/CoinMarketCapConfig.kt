package com.desafiomercadobitcoin.data.network

/**
 * Configuração do provedor. A chave **não** mora aqui: ela é injetada a partir do
 * `BuildConfig` do `:app`, porque `:data` não pode conhecer o módulo de aplicação.
 */
data class CoinMarketCapConfig(
    val apiKey: String,
    val isDebug: Boolean = false,
) {
    companion object {
        const val BASE_URL: String = "https://pro-api.coinmarketcap.com/"
        const val API_KEY_HEADER: String = "X-CMC_PRO_API_KEY"
        const val TIMEOUT_MILLIS: Long = 15_000L
    }
}
