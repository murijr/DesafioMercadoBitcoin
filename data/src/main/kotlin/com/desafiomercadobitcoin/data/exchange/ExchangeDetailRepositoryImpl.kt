package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.mapper.toBM
import com.desafiomercadobitcoin.data.exchange.mapper.toDetailBM
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.exchange.ExchangeDetailRepository
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail

/**
 * Dois métodos independentes sobre o mesmo `exchangeId`, sem estado a manter — ao contrário
 * de [ExchangeRepositoryImpl], que memoiza o índice da listagem (D3 de `add-exchange-detail`).
 */
class ExchangeDetailRepositoryImpl(
    private val remote: ExchangeRemoteDataSource,
) : ExchangeDetailRepository {
    override suspend fun loadDetail(exchangeId: Int): BMExchangeDetail {
        val content = remote.loadInfo(listOf(exchangeId))
        // Ausência da chave é a única distinção de "id inexistente" que este passo consegue
        // fazer sem depender do 400 genérico do provedor (ver Open Questions do design.md).
        return content[exchangeId.toString()]?.toDetailBM() ?: throw DomainError.NotFound()
    }

    /**
     * O provedor devolve `/v1/exchange/assets` por **carteira**, não por moeda: a mesma
     * moeda aparece uma vez por carteira que a possui. Reduzimos para "a *exchange* negocia
     * esta moeda" — sem isso, a mesma moeda repetida quebra a chave exigida pelo `LazyColumn`
     * na apresentação.
     *
     * A comparação é case-insensitive e ignora espaços nas pontas: o *spec* do *payload* é
     * "mesma moeda" quando o nome é o mesmo ignorando caixa e *whitespace*, e o endpoint já
     * devolveu variantes como `"USDD"` × `"usdd"` e `"Avantis"` × `"Avantis "` em carteiras
     * distintas da mesma *exchange* — manter a primeira ocorrência preserva o `priceUsd` da
     * carteira que o provedor escolheu como canônica. O nome retido é sempre `trim()`ado, mesmo
     * quando já é o único candidato, pra nunca vazar espaço nas pontas pra apresentação. Ver
     * `ExchangeDetailRepositoryImplTest` para o caso de regressão.
     */
    override suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency> =
        remote
            .loadAssets(exchangeId)
            .map { it.toBM() }
            .distinctBy { it.name.normalizeForDeduplication() }
            .map { it.copy(name = it.name.trim()) }

    private fun String.normalizeForDeduplication(): String = trim().lowercase()
}
