/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.stage

import cloud.orbit.common.logging.logger
import kotlin.coroutines.CoroutineContext

class ErrorHandler {
    private val logger by logger()

    @Suppress("UNUSED_PARAMETER")
    fun onUnhandledException(coroutineContext: CoroutineContext, throwable: Throwable) =
        onUnhandledException(throwable)

    fun onUnhandledException(throwable: Throwable) {
        logger.error("Unhandled exception in Orbit.", throwable)
    }
}