/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.concurrent

import kotlinx.coroutines.CoroutineDispatcher

internal data class RuntimePools(
    val cpuPool: CoroutineDispatcher,
    val ioPool: CoroutineDispatcher
)