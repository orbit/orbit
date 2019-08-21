/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.concurrent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

private class SupervisorScope(
    private val pool: CoroutineDispatcher,
    private val exceptionHandler: (CoroutineContext, Throwable) -> Unit
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job +
                pool +
                CoroutineExceptionHandler(exceptionHandler)
}

internal class RuntimeScopes(
    runtimePools: RuntimePools,
    exceptionHandler: (CoroutineContext, Throwable) -> Unit
) {
    val cpuScope: CoroutineScope = SupervisorScope(runtimePools.cpuPool, exceptionHandler)
    val ioScope: CoroutineScope = SupervisorScope(runtimePools.ioPool, exceptionHandler)
}