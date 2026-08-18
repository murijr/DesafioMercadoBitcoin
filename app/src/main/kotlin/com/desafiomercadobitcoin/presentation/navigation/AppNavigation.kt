package com.desafiomercadobitcoin.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.desafiomercadobitcoin.presentation.feature.exchangedetail.ExchangeDetailScreen
import com.desafiomercadobitcoin.presentation.feature.exchangelist.ExchangeListRoute

/**
 * Casca de navegação. A pilha é o **único** lugar que decide qual destino está visível.
 *
 * Os decoradores dão a cada entrada seu próprio escopo: o de estado salvável preserva a
 * posição de rolagem ao empilhar por cima, e o de `ViewModelStore` faz o `ViewModel` morrer
 * junto com a entrada que o criou.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey> = rememberNavBackStack(ExchangeListKey),
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<ExchangeListKey> {
                    ExchangeListRoute(
                        onExchangeSelected = { id -> backStack.add(ExchangeDetailKey(id)) },
                    )
                }
                entry<ExchangeDetailKey> { key ->
                    ExchangeDetailScreen(exchangeId = key.exchangeId)
                }
            },
    )
}
