package com.desafiomercadobitcoin.presentation.feature.exchangelist

import androidx.lifecycle.SavedStateHandle
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.TextKey
import com.desafiomercadobitcoin.domain.exchange.GetExchangePageUseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMExchange
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangePage
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class ExchangeListViewModelTest {
    abstract class TestSetup {
        protected val getExchangePage = mockk<GetExchangePageUseCase>()
        protected val resources = mockk<ResourceProvider>()
        protected val savedStateHandle = SavedStateHandle()
        protected val firstBatchIds = listOf(1, 2)
        protected val secondBatchIds = listOf(THIRD_ID, THIRD_ID + 1)
        private val dispatcher = StandardTestDispatcher()

        @Before
        fun setUpDispatcherAndResources() {
            Dispatchers.setMain(dispatcher)
            every { resources.resolve(any<TextKey>()) } answers { "texto:${firstArg<TextKey>()}" }
            every { resources.resolve(any<Int>()) } answers { "res:${firstArg<Int>()}" }
        }

        @After
        fun tearDown() {
            Dispatchers.resetMain()
            stopKoin()
        }

        protected fun viewModel() = ExchangeListViewModel(getExchangePage, resources, savedStateHandle)

        protected fun page(
            ids: List<Int>,
            index: Int = FIRST_PAGE,
            hasMore: Boolean = true,
        ) = BMExchangePage(
            items = ids.map { exchange(it) },
            page = index,
            hasMore = hasMore,
        )

        protected fun exchange(id: Int) =
            BMExchange(
                id = id,
                name = "Exchange $id",
                logoUrl = null,
                spotVolumeUsd = null,
                dateLaunched = null,
            )
    }

    @RunWith(RobolectricTestRunner::class)
    class HappyPath : TestSetup() {
        @Test
        fun `given the screen is opened when the first page arrives then its content is published`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(firstBatchIds))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    listOf(1, 2),
                    viewModel.state.value.items
                        .map { it.id },
                )
                assertFalse(viewModel.state.value.isLoading)
                assertNull(viewModel.state.value.errorMessage)
            }

        @Test
        fun `given a loaded page when scrolling then the next batch is appended at the end`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(firstBatchIds))
                coEvery { getExchangePage.execute(SECOND_PAGE) } returns
                    Result.success(page(secondBatchIds, index = SECOND_PAGE))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()
                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                assertEquals(
                    firstBatchIds + secondBatchIds,
                    viewModel.state.value.items
                        .map { it.id },
                )
                assertEquals(SECOND_PAGE, viewModel.state.value.page)
            }

        @Test
        fun `given a batch in flight when another is requested then no duplicate call is issued`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(listOf(1)))
                coEvery { getExchangePage.execute(SECOND_PAGE) } coAnswers {
                    delay(SLOW_ANSWER_MILLIS)
                    Result.success(page(listOf(2), index = SECOND_PAGE))
                }
                val viewModel = viewModel()
                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                viewModel.send(ExchangeListEvent.NextPageRequested)
                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                coVerify(exactly = 1) { getExchangePage.execute(SECOND_PAGE) }
                coVerify(exactly = 0) { getExchangePage.execute(THIRD_PAGE) }
                assertEquals(
                    listOf(1, 2),
                    viewModel.state.value.items
                        .map { it.id },
                )
            }

        @Test
        fun `given the end of the catalog when scrolling then no further page is requested`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns
                    Result.success(page(listOf(1), hasMore = false))
                val viewModel = viewModel()
                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                coVerify(exactly = 0) { getExchangePage.execute(SECOND_PAGE) }
                assertFalse(viewModel.state.value.isLoadingMore)
            }

        @Test
        fun `given an empty index when it is loaded then the empty state is published`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns
                    Result.success(page(emptyList(), hasMore = false))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                assertTrue(viewModel.state.value.isEmpty)
                assertNull(viewModel.state.value.errorMessage)
                assertFalse(viewModel.state.value.isLoading)
            }

        @Test
        fun `given an item is selected when the event arrives then the detail effect is emitted`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(listOf(SELECTED_ID)))
                val viewModel = viewModel()
                val effects = mutableListOf<ExchangeListEffect>()

                val collector = launch { viewModel.effect.toList(effects) }
                runCurrent()

                viewModel.send(ExchangeListEvent.ExchangeSelected(exchangeId = SELECTED_ID))
                advanceUntilIdle()
                collector.cancel()

                assertEquals(listOf(ExchangeListEffect.OpenExchangeDetail(SELECTED_ID)), effects)
            }

        @Test
        fun `given a loaded page when it is published then the reached page is retained`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(listOf(1)))
                coEvery { getExchangePage.execute(SECOND_PAGE) } returns
                    Result.success(page(listOf(2), index = SECOND_PAGE, hasMore = false))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()
                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                assertEquals(SECOND_PAGE, savedStateHandle.get<Int>(ExchangeListViewModel.KEY_REACHED_PAGE))
                assertTrue(savedStateHandle.keys().none { it == "items" })
            }

        @Test
        fun `given a retained page when the screen is recreated then it is loaded back`() =
            runTest {
                savedStateHandle[ExchangeListViewModel.KEY_REACHED_PAGE] = SECOND_PAGE
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(firstBatchIds))
                coEvery { getExchangePage.execute(SECOND_PAGE) } returns
                    Result.success(page(listOf(THIRD_ID), index = SECOND_PAGE, hasMore = false))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    firstBatchIds + THIRD_ID,
                    viewModel.state.value.items
                        .map { it.id },
                )
            }
    }

    @RunWith(RobolectricTestRunner::class)
    class ErrorPath : TestSetup() {
        @Test
        fun `given the first page fails when the screen is opened then a localized error is published`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    resources.resolve(DomainError.Network.textKey),
                    viewModel.state.value.errorMessage,
                )
                assertTrue(
                    viewModel.state.value.items
                        .isEmpty(),
                )
                assertFalse(viewModel.state.value.isLoading)
            }

        @Test
        fun `given a failed first page when the retry is triggered then the loading restarts`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()
                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(listOf(1)))
                viewModel.send(ExchangeListEvent.RetryRequested)
                advanceUntilIdle()

                assertEquals(
                    listOf(1),
                    viewModel.state.value.items
                        .map { it.id },
                )
                assertNull(viewModel.state.value.errorMessage)
            }

        @Test
        fun `given a later batch fails when it is loaded then the published items are preserved`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(firstBatchIds))
                coEvery { getExchangePage.execute(SECOND_PAGE) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()
                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                assertEquals(
                    listOf(1, 2),
                    viewModel.state.value.items
                        .map { it.id },
                )
                assertNull(viewModel.state.value.errorMessage)
                assertEquals(
                    resources.resolve(DomainError.Network.textKey),
                    viewModel.state.value.pagingErrorMessage,
                )
            }

        @Test
        fun `given a failed later batch when the retry is triggered then only that batch is asked again`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns Result.success(page(listOf(1)))
                coEvery { getExchangePage.execute(SECOND_PAGE) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()
                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()
                viewModel.send(ExchangeListEvent.NextPageRequested)
                advanceUntilIdle()

                coEvery { getExchangePage.execute(SECOND_PAGE) } returns
                    Result.success(page(listOf(2), index = SECOND_PAGE, hasMore = false))
                viewModel.send(ExchangeListEvent.RetryRequested)
                advanceUntilIdle()

                coVerify(exactly = 1) { getExchangePage.execute(FIRST_PAGE) }
                assertEquals(
                    listOf(1, 2),
                    viewModel.state.value.items
                        .map { it.id },
                )
                assertNull(viewModel.state.value.pagingErrorMessage)
            }

        @Test
        fun `given an untyped failure when the first page fails then the unexpected text is published`() =
            runTest {
                coEvery { getExchangePage.execute(FIRST_PAGE) } returns
                    Result.failure(IllegalStateException("nao tipado"))
                val viewModel = viewModel()

                viewModel.send(ExchangeListEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    resources.resolve(DomainError.Unexpected.textKey),
                    viewModel.state.value.errorMessage,
                )
            }
    }
}

private const val SLOW_ANSWER_MILLIS = 100L
private const val FIRST_PAGE = 0
private const val SECOND_PAGE = 1
private const val THIRD_PAGE = 2
private const val SELECTED_ID = 7
private const val THIRD_ID = 3
