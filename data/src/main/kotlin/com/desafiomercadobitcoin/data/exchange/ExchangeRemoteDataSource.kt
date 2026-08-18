package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.api.ExchangeInfoRoute
import com.desafiomercadobitcoin.data.exchange.api.ExchangeMapRoute
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfoResponse
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeMapEntry
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeMapResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get

/**
 * IO e nada mais: monta a requisição, devolve o `DM` cru e sinaliza falha por lançamento.
 * Quem converte `Throwable` em `Result` é o `UseCase`.
 */
class ExchangeRemoteDataSource(
    private val client: HttpClient,
) {
    suspend fun loadActiveIndex(): List<DMExchangeMapEntry> =
        client.get(ExchangeMapRoute()).body<DMExchangeMapResponse>().data

    /**
     * O limite de ids por consulta é do provedor, então é aqui que ele é respeitado — o
     * chamador não carrega essa responsabilidade. Conjunto vazio não vira requisição:
     * não há o que perguntar.
     */
    suspend fun loadInfo(ids: List<Int>): Map<String, DMExchangeInfo> {
        if (ids.isEmpty()) return emptyMap()
        require(ids.size <= MAX_IDS_PER_REQUEST) {
            "o provedor aceita no maximo $MAX_IDS_PER_REQUEST ids por consulta, recebeu ${ids.size}"
        }
        return client
            .get(ExchangeInfoRoute(id = ids.joinToString(separator = ",")))
            .body<DMExchangeInfoResponse>()
            .data
    }

    companion object {
        const val MAX_IDS_PER_REQUEST: Int = 100
    }
}
