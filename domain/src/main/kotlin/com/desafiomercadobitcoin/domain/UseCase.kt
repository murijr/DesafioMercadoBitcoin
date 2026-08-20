package com.desafiomercadobitcoin.domain

import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.toDomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
