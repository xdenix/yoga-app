package com.example.yogaapp.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test [AppDispatchers] backed by an [UnconfinedTestDispatcher] so that ViewModels
 * execute synchronously in tests without `runBlocking`.
 */
class TestAppDispatchers(
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : AppDispatchers {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val unconfined: CoroutineDispatcher = dispatcher
}
