/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.logging.impl

import cloud.orbit.common.logging.Logger
import org.slf4j.Logger as SLF4JLogger

internal class OrbitDefaultLogger(private val loggerWrapper: SLF4JLogger) : Logger {
    override val isTraceEnabled: Boolean
        get() = loggerWrapper.isTraceEnabled

    override val isDebugEnabled: Boolean
        get() = loggerWrapper.isDebugEnabled

    override val isInfoEnabled: Boolean
        get() = loggerWrapper.isInfoEnabled

    override val isWarnEnabled: Boolean
        get() = loggerWrapper.isWarnEnabled

    override val isErrorEnabled: Boolean
        get() = loggerWrapper.isErrorEnabled

    override fun trace(msg: String, vararg arg: Any?) {
        loggerWrapper.trace(msg, *arg)
    }

    override fun debug(msg: String, vararg arg: Any?) {
        loggerWrapper.debug(msg, *arg)
    }

    override fun info(msg: String, vararg arg: Any?) {
        loggerWrapper.info(msg, *arg)
    }

    override fun warn(msg: String, vararg arg: Any?) {
        loggerWrapper.warn(msg, *arg)
    }

    override fun error(msg: String, vararg arg: Any?) {
        loggerWrapper.error(msg, *arg)
    }
}