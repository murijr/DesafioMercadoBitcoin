package com.desafiomercadobitcoin.data.exchange

import com.desafiomercadobitcoin.data.exchange.mapper.toBM
import com.desafiomercadobitcoin.domain.exchange.ExchangeRepository
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Compõe o índice de corretoras ativas com o conteúdo exibível de cada lote.
 *
 * O índice é obtido uma única vez e guardado em memória sob [Mutex] — rolagem rápida
 * dispara páginas em sequência, e sem a exclusão mútua duas delas buscariam o índice.
 * A memoização morre com o processo, que é exatamente o tempo de vida desejado (D1).
 */
class ExchangeRepositoryImpl(
    private val remote: ExchangeRemoteDataSource,
) : ExchangeRepository {
    private val indexMutex = Mutex()
    private var activeIds: List<Int>? = null

    override suspend fun loadPage(page: Int): BMExchangePage {
        val ids = activeIds()
        val from = page * PAGE_SIZE
        if (from >= ids.size) return BMExchangePage(items = emptyList(), page = page, hasMore = false)

        val until = minOf(from + PAGE_SIZE, ids.size)
        val batch = ids.subList(from, until)
        val content = remote.loadInfo(batch)

        return BMExchangePage(
            // Percorrer o lote, e não o conteúdo devolvido, é o que preserva a ordem do
            // índice e omite o id que veio sem conteúdo correspondente.
            items = batch.mapNotNull { id -> content[id.toString()]?.toBM() },
            page = page,
            hasMore = until < ids.size,
        )
    }

    private suspend fun activeIds(): List<Int> =
        indexMutex.withLock {
            activeIds ?: remote.loadActiveIndex().map { it.id }.also { activeIds = it }
        }

    private companion object {
        /** Igual ao limite de ids por consulta do provedor: um lote, uma requisição. */
        const val PAGE_SIZE = ExchangeRemoteDataSource.MAX_IDS_PER_REQUEST
    }
}
