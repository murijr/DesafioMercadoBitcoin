package com.desafiomercadobitcoin.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * G2 — guardas sobre a chave de `LazyColumn`/`LazyRow`/`LazyVerticalGrid`/`LazyHorizontalGrid`.
 *
 * O `LazyColumn` exige chave única e estável para cada item: sem ela, ou com chave colidindo
 * com a de outro item (ex.: nome de ativo que o provedor devolve em mais de uma carteira),
 * a `SubcomposeLayout` lança `IllegalArgumentException: Key "X" was already used` e o app
 * crasha com `FATAL EXCEPTION: main` (regressão observada com a chave `it.name` na
 * listagem de moedas da *exchange*).
 *
 * Android Lint não cobre isso — `IllegalArgumentException` em tempo de execução não vira
 * aviso estático. Daí a regra morar aqui, no G2.
 *
 * Política da chave:
 *  - **PK da fonte** (`it.id`, `it.<field>Id` quando é PK da fonte) — caminho feliz e
 *    única chave estável quando a lista muta (refresh, paginação, remoção).
 *  - **Índice puro** ou **composto com índice** — *safety net* aceitável quando o
 *    repositório não consegue garantir unicidade; paga o preço de perder composição/
 *    `remember` entre mutações, mas não trava.
 *  - **Campo cosmético** (`it.name`, `it.symbol`, `it.slug`, `it.label`, ...) — proibido:
 *    é exatamente o que causou a regressão.
 */
class LazyColumnKeyRulesTest {
    /**
     * Lista `items(...)` / `itemsIndexed(...)` em escopo `LazyListScope`/`LazyGridScope`
     * dentro de `:app` precisa ter `key =` declarado. Sem chave, o `LazyColumn` usa a
     * posição na lista como identidade e qualquer inserção/remoção invalida tudo.
     */
    @Test
    fun `lazy list items in app must declare a key`() {
        Konsist
            .scopeFromDirectory("app/src/main")
            .functions()
            .filter { function ->
                function.text.contains("items(") || function.text.contains("itemsIndexed(")
            }.assertTrue { it.text.contains("key =") }
    }

    /**
     * A chave de `items(...)` / `itemsIndexed(...)` em `:app` precisa apontar para uma
     * **PK da fonte** (campo `id` / `Id` do item) **ou** para o **índice** da posição.
     * Chaves em campo cosmético (`name`, `symbol`, `slug`, `label`) são proibidas: o
     * provedor pode devolver a mesma string em mais de um item.
     *
     * Esta regra é estrita por design — melhor um falso positivo em uma chave legítima
     * que um crash em produção por uma chave frouxa. A lista do que é permitido vive no
     * regex `allowedKey` declarado dentro do teste; qualquer outra forma é reprovada.
     */
    @Test
    fun `lazy list keys must be a primary key or the index`() {
        // PK da fonte: `.id` ou `.<Algo>Id` (camelCase terminando em `Id`).
        // Indice: `index` ou `i` como parametro, sozinho ou compondo a chave.
        // PK simples via `.id` OU PK composta via `.XxxId` (camelCase terminando em `Id`).
        // Indice: `index` ou `i` como parametro, sozinho ou compondo a chave.
        val allowedKey =
            Regex(
                """key\s*=\s*\{[^}]*(?:\b(?:index|i)\b|\.id\b|\.[A-Za-z][a-zA-Z0-9_]*[Ii]d\b)""",
            )

        Konsist
            .scopeFromDirectory("app/src/main")
            .functions()
            .filter { function ->
                function.text.contains("items(") || function.text.contains("itemsIndexed(")
            }.assertTrue { function ->
                val text = function.text
                if (!text.contains("key =")) return@assertTrue true // outra regra cuida disso
                val keyBlock = Regex("""key\s*=\s*\{[^}]*\}""").find(text)?.value.orEmpty()
                allowedKey.containsMatchIn(keyBlock)
            }
    }
}
