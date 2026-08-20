package com.desafiomercadobitcoin.domain

import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.toDomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base de toda regra de negócio executável.
 *
 * Contrato: nada escapa como exceção crua para o chamador, exceto o cancelamento — que é
 * re-lançado antes de qualquer captura genérica, em respeito à structured concurrency.
 *
 */
abstract class UseCase<I, S> {
    protected abstract suspend fun doExecute(input: I): S

    suspend fun execute(input: I): Result<S> =
        withContext(Dispatchers.Default) {
            try {
                Result.success(doExecute(input))
            } catch (error: CancellationException) {
                throw error
            } catch (error: DomainError) {
                Result.failure(error)
            } catch (error: Throwable) {
                Result.failure(error.toDomainError())
            }
        }
}
