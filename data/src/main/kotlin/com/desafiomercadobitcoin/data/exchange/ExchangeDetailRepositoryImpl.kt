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
        return content[exchangeId.toString()]?.toDetailBM() ?: throw DomainError.NotFound
    }

    /**
     * O provedor devolve `/v1/exchange/assets` por **carteira**, não por moeda: a mesma
     * moeda aparece uma vez por carteira que a possui. `distinctBy` reduz para "a *exchange*
     * negocia esta moeda", que é o que o *spec* pede — sem isso, a mesma moeda repetida quebra
     * a chave exigida pelo `LazyColumn` na apresentação (confirmado ao vivo contra a API real,
     * uma *exchange* com `USDD` em mais de uma carteira).
     */
    override suspend fun loadCurrencies(exchangeId: Int): List<BMCurrency> =
        remote.loadAssets(exchangeId).map { it.toBM() }.distinctBy { it.name }
}
