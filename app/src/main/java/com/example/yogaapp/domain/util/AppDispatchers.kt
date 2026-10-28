package com.example.yogaapp.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Wrapper around [Dispatchers] so that unit tests can substitute a [TestDispatcher].
 * Injected via Hilt (see [com.example.yogaapp.data.di.DispatcherModule]).
 *
 * Why a wrapper instead of using Dispatchers.IO directly? So that use cases and the
 * repository can be tested without `runBlocking` and without UnconfinedTestDispatcher
 * leaking into production code.
 */
interface AppDispatchers {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/** Default production dispatchers — used when no override is bound in Hilt. */
class DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
