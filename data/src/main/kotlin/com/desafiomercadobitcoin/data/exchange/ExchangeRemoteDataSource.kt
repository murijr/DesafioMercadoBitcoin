package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.api.ExchangeAssetsRoute
import com.desafiomercadobitcoin.data.exchange.api.ExchangeInfoRoute
import com.desafiomercadobitcoin.data.exchange.api.ExchangeMapRoute
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAsset
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeAssetsResponse
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfo
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeInfoResponse
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeMapEntry
import com.desafiomercadobitcoin.data.exchange.dto.DMExchangeMapResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * IO e nada mais: monta a requisição, devolve o `DM` cru e sinaliza falha por lançamento.
 * Quem converte `Throwable` em `Result` é o `UseCase`.
 *
 * O cliente chega **preguiçoso** e o IO roda em `Dispatchers.IO`; as duas coisas só
 * funcionam juntas. Recebê-lo pronto o faria nascer quando o Koin resolve esta classe — o
 * que acontece na composição, na main thread —, e ali o `install(Resources)` inicializa o
 * `kotlin-reflect`, que lê o APK do disco. Só a preguiça adiaria essa leitura para a
 * primeira chamada, que também começa na main thread (`viewModelScope` é `Main.immediate`);
 * só o `withContext` não ajudaria, porque a construção já teria acontecido antes de
 * qualquer `suspend`. Juntos, o primeiro toque no cliente cai dentro do dispatcher de IO.
 */
class ExchangeRemoteDataSource(
    client: Lazy<HttpClient>,
) {
    private val client by client

    suspend fun loadActiveIndex(): List<DMExchangeMapEntry> =
        withContext(Dispatchers.IO) {
            client.get(ExchangeMapRoute()).body<DMExchangeMapResponse>().data
        }

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
        return withContext(Dispatchers.IO) {
            client
                .get(ExchangeInfoRoute(id = ids.joinToString(separator = ",")))
                .body<DMExchangeInfoResponse>()
                .data
        }
    }

    /** As moedas negociadas por uma corretora. O provedor devolve o conjunto inteiro numa única resposta. */
    suspend fun loadAssets(exchangeId: Int): List<DMExchangeAsset> =
        withContext(Dispatchers.IO) {
            client
                .get(ExchangeAssetsRoute(id = exchangeId.toString()))
                .body<DMExchangeAssetsResponse>()
                .data
        }

    companion object {
        const val MAX_IDS_PER_REQUEST: Int = 100
    }
}
