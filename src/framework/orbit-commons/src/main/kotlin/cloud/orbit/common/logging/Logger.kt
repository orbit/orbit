/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.logging

/**
 * Logger API.
 */
interface Logger {
    /**
     * Determines whether TRACE level logging is enabled.
     */
    val isTraceEnabled: Boolean

    /**
     * Determines whether DEBUG level logging is enabled.
     */
    val isDebugEnabled: Boolean

    /**
     * Determines whether INFO level logging is enabled.
     */
    val isInfoEnabled: Boolean

    /**
     * Determines whether WARN level logging is enabled.
     */
    val isWarnEnabled: Boolean

    /**
     * Determines whether ERROR level logging is enabled.
     */
    val isErrorEnabled: Boolean

    /**
     * Logs the specified message with the TRACE log level.
     * @param msg The message.
     * @param arg The arguments.
     */
    fun trace(msg: String, vararg arg: Any?)

    /**
     * Logs the specified message with the DEBUG log level.
     * @param msg The message.
     * @param arg The arguments.
     */
    fun debug(msg: String, vararg arg: Any?)

    /**
     * Logs the specified message with the INFO log level.
     * @param msg The message.
     * @param arg The arguments.
     */
    fun info(msg: String, vararg arg: Any?)

    /**
     * Logs the specified message with the WARN log level.
     * @param msg The message.
     * @param arg The arguments.
     */
    fun warn(msg: String, vararg arg: Any?)

    /**
     * Logs the specified message with the ERROR log level.
     * @param msg The message.
     * @param arg The arguments.
     */
    fun error(msg: String, vararg arg: Any?)
}

/**
 * Lazily logs the specified message with the TRACE log level.
 * The message is only computed if the log level is enabled.
 * @param msg The message.
 */
inline fun Logger.trace(msg: () -> String) {
    if(isTraceEnabled) {
        trace(msg())
    }
}

/**
 * Lazily logs the specified message with the DEBUG log level.
 * The message is only computed if the log level is enabled.
 * @param msg The message.
 */
inline fun Logger.debug(msg: () -> String) {
    if(isDebugEnabled) {
        debug(msg())
    }
}

/**
 * Lazily logs the specified message with the INFO log level.
 * The message is only computed if the log level is enabled.
 * @param msg The message.
 */
inline fun Logger.info(msg: () -> String) {
    if(isInfoEnabled) {
        info(msg())
    }
}

/**
 * Lazily logs the specified message with the WARN log level.
 * The message is only computed if the log level is enabled.
 * @param msg The message.
 */
inline fun Logger.warn(msg: () -> String) {
    if(isWarnEnabled) {
        warn(msg())
    }
}

/**
 * Lazily logs the specified message the ERROR log level.
 * The message is only computed if the log level is enabled.
 * @param msg The message.
 */
inline fun Logger.error(msg: () -> String) {
    if(isErrorEnabled) {
        error(msg())
    }
}

