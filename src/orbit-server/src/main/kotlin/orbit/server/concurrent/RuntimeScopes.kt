/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.concurrent

import kotlinx.coroutines.CoroutineScope
import orbit.util.concurrent.SupervisorScope
import kotlin.coroutines.CoroutineContext

class RuntimeScopes(
    runtimePools: RuntimePools,
    exceptionHandler: (CoroutineContext, Throwable) -> Unit
) {
    val cpuScope: CoroutineScope = SupervisorScope(runtimePools.cpuPool, exceptionHandler)
    val ioScope: CoroutineScope = SupervisorScope(runtimePools.ioPool, exceptionHandler)
}