package com.desafiomercadobitcoin.data.network

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
