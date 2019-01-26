/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.concurrent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class SupervisorScope(
    val cpuPool: CoroutineDispatcher,
    val ioPool: CoroutineDispatcher,
    private val exceptionHandler: (CoroutineContext, Throwable) -> Unit
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job +
                cpuPool +
                CoroutineExceptionHandler(exceptionHandler)
}