package com.desafiomercadobitcoin

import android.os.StrictMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

/**
 * G10 — metade JVM: prova que a politica de *thread* e **instalada** no arranque.
 *
 * A `Application` real sobe sob Robolectric antes do teste, entao `onCreate` ja rodou e o
 * teste so observa o resultado — mesmo apoio que `di/AppGraphTest` usa para o Koin.
 *
 * Que **ninguem viola** a politica so um dispositivo prova: a fiscalizacao depende do
 * `BlockGuard` nativo, inerte na JVM. Essa metade e a suite instrumentada (G9), que roda
 * sobre um processo com `penaltyDeath` armado.
 *
 * A comparacao e por `toString()` porque `ThreadPolicy` nao implementa `equals` e
 * `getThreadPolicy()` devolve uma instancia nova a cada chamada — o texto carrega a mascara,
 * que e o que distingue uma politica da outra.
 */
@RunWith(RobolectricTestRunner::class)
class DesafioApplicationTest {
    /**
     * Subir a `Application` real tambem inicia o Koin, e o contexto global e estatico por
     * classloader: sem esta limpeza, as classes de teste seguintes reprovariam com
     * `KoinApplicationAlreadyStartedException`. Mesmo cuidado de `di/AppGraphTest`.
     */
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `given the debug application when it starts then the strict thread policy is installed`() {
        val expected =
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDeath()
                .build()

        assertEquals(expected.toString(), StrictMode.getThreadPolicy().toString())
    }
}
