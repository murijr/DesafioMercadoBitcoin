package com.desafiomercadobitcoin.domain.exchange

import com.desafiomercadobitcoin.domain.UseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage

/**
 * Entrega a página `input` do catálogo de corretoras.
 *
 * Não há regra além da delegação: a composição do índice com o conteúdo é do repositório,
 * e a tradução de falha vem de [UseCase]. O caso de uso existe para que a apresentação
 * dependa de um `UseCase`, como o G2 exige, e não do contrato de repositório.
 */
class GetExchangePageUseCase(
    private val repository: ExchangeRepository,
) : UseCase<Int, BMExchangePage>() {
    override suspend fun doExecute(input: Int): BMExchangePage = repository.loadPage(input)
}
