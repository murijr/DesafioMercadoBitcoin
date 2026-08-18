package com.desafiomercadobitcoin.presentation.feature.exchangedetail

import androidx.lifecycle.SavedStateHandle
import com.desafiomercadobitcoin.domain.error.DomainError
import com.desafiomercadobitcoin.domain.error.TextKey
import com.desafiomercadobitcoin.domain.exchange.GetExchangeCurrenciesUseCase
import com.desafiomercadobitcoin.domain.exchange.GetExchangeDetailUseCase
import com.desafiomercadobitcoin.domain.exchange.model.BMCurrency
import com.desafiomercadobitcoin.domain.exchange.model.BMExchangeDetail
import com.desafiomercadobitcoin.presentation.common.ResourceProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class ExchangeDetailViewModelTest {
    abstract class TestSetup {
        protected val getExchangeDetail = mockk<GetExchangeDetailUseCase>()
        protected val getExchangeCurrencies = mockk<GetExchangeCurrenciesUseCase>()
        protected val resources = mockk<ResourceProvider>()
        protected val dispatcher = StandardTestDispatcher()

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

        protected fun viewModel(exchangeId: Int = EXCHANGE_ID) =
            ExchangeDetailViewModel(
                getExchangeDetail,
                getExchangeCurrencies,
                resources,
                SavedStateHandle(mapOf(ExchangeDetailViewModel.KEY_EXCHANGE_ID to exchangeId)),
            )

        protected fun detail(id: Int = EXCHANGE_ID) =
            BMExchangeDetail(
                id = id,
                name = "Binance",
                logoUrl = null,
                description = null,
                websiteUrl = null,
                makerFee = null,
                takerFee = null,
                dateLaunched = null,
            )

        protected fun currency(name: String) = BMCurrency(name = name, priceUsd = 1.0)
    }

    @RunWith(RobolectricTestRunner::class)
    class HappyPath : TestSetup() {
        @Test
        fun `given an empty saved state when the id is ensured then it is stored`() {
            val savedStateHandle = SavedStateHandle()
            val viewModel =
                ExchangeDetailViewModel(getExchangeDetail, getExchangeCurrencies, resources, savedStateHandle)

            viewModel.ensureExchangeId(EXCHANGE_ID)

            assertEquals(EXCHANGE_ID, savedStateHandle.get<Int>(ExchangeDetailViewModel.KEY_EXCHANGE_ID))
        }

        @Test
        fun `given an already stored id when a different one is ensured then the original is kept`() {
            val savedStateHandle = SavedStateHandle(mapOf(ExchangeDetailViewModel.KEY_EXCHANGE_ID to EXCHANGE_ID))
            val viewModel =
                ExchangeDetailViewModel(getExchangeDetail, getExchangeCurrencies, resources, savedStateHandle)

            viewModel.ensureExchangeId(EXCHANGE_ID + 1)

            assertEquals(EXCHANGE_ID, savedStateHandle.get<Int>(ExchangeDetailViewModel.KEY_EXCHANGE_ID))
        }

        @Test
        fun `given the screen is opened when both requests succeed then detail and currencies are published`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.success(detail())
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns
                    Result.success(listOf(currency("Bitcoin")))
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    EXCHANGE_ID,
                    viewModel.state.value.detail
                        ?.id,
                )
                assertEquals(
                    listOf("Bitcoin"),
                    viewModel.state.value.currencies
                        .map { it.name },
                )
                assertFalse(viewModel.state.value.isLoadingDetail)
                assertFalse(viewModel.state.value.isLoadingCurrencies)
            }

        @Test
        fun `given the detail succeeds when currencies fail then the detail remains displayed`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.success(detail())
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    EXCHANGE_ID,
                    viewModel.state.value.detail
                        ?.id,
                )
                assertEquals(
                    resources.resolve(DomainError.Network.textKey),
                    viewModel.state.value.currenciesErrorMessage,
                )
            }

        @Test
        fun `given no currencies when both requests succeed then the empty state is published`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.success(detail())
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.success(emptyList())
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertTrue(viewModel.state.value.isCurrenciesEmpty)
                assertNull(viewModel.state.value.currenciesErrorMessage)
            }

        @Test
        fun `given the two requests when the screen opens then neither waits for the other`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } coAnswers {
                    delay(SLOW_ANSWER_MILLIS)
                    Result.success(detail())
                }
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns
                    Result.success(listOf(currency("Bitcoin")))
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                dispatcher.scheduler.advanceTimeBy(1)
                dispatcher.scheduler.runCurrent()

                assertEquals(
                    listOf("Bitcoin"),
                    viewModel.state.value.currencies
                        .map { it.name },
                )
                assertNull(viewModel.state.value.detail)

                advanceUntilIdle()
                assertEquals(
                    EXCHANGE_ID,
                    viewModel.state.value.detail
                        ?.id,
                )
            }

        @Test
        fun `given a failed detail retry when it is triggered then the detail is loaded`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.failure(DomainError.Network)
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.success(emptyList())
                val viewModel = viewModel()
                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.success(detail())
                viewModel.send(ExchangeDetailEvent.RetryDetailRequested)
                advanceUntilIdle()

                assertEquals(
                    EXCHANGE_ID,
                    viewModel.state.value.detail
                        ?.id,
                )
                assertNull(viewModel.state.value.detailErrorMessage)
            }

        @Test
        fun `given a failed currencies retry when it is triggered then only currencies are loaded again`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.success(detail())
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.failure(DomainError.Network)
                val viewModel = viewModel()
                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns
                    Result.success(listOf(currency("Bitcoin")))
                viewModel.send(ExchangeDetailEvent.RetryCurrenciesRequested)
                advanceUntilIdle()

                assertEquals(
                    listOf("Bitcoin"),
                    viewModel.state.value.currencies
                        .map { it.name },
                )
                assertNull(viewModel.state.value.currenciesErrorMessage)
            }
    }

    @RunWith(RobolectricTestRunner::class)
    class ErrorPath : TestSetup() {
        @Test
        fun `given the currencies succeed when the detail fails then no currency list is displayed`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.failure(DomainError.Network)
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns
                    Result.success(listOf(currency("Bitcoin")))
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertNull(viewModel.state.value.detail)
                assertEquals(
                    resources.resolve(DomainError.Network.textKey),
                    viewModel.state.value.detailErrorMessage,
                )
            }

        @Test
        fun `given the detail is not found when the screen opens then the not found state is published`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns Result.failure(DomainError.NotFound)
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.success(emptyList())
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertTrue(viewModel.state.value.isDetailNotFound)
                assertEquals(
                    resources.resolve(DomainError.NotFound.textKey),
                    viewModel.state.value.detailErrorMessage,
                )
            }

        @Test
        fun `given an untyped failure when the detail fails then the unexpected text is published`() =
            runTest {
                coEvery { getExchangeDetail.execute(EXCHANGE_ID) } returns
                    Result.failure(IllegalStateException("nao tipado"))
                coEvery { getExchangeCurrencies.execute(EXCHANGE_ID) } returns Result.success(emptyList())
                val viewModel = viewModel()

                viewModel.send(ExchangeDetailEvent.ScreenOpened)
                advanceUntilIdle()

                assertEquals(
                    resources.resolve(DomainError.Unexpected.textKey),
                    viewModel.state.value.detailErrorMessage,
                )
            }
    }
}

private const val EXCHANGE_ID = 270
private const val SLOW_ANSWER_MILLIS = 100L
