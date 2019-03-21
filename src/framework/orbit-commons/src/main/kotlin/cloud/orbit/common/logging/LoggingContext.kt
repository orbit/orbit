/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.logging

import org.slf4j.MDC

/**
 * Provides structured logging features for Orbit when available in the
 * underlying logging implementation.
 */
object LoggingContext {

    /**
     * Puts a context key with the specified value.
     * @param key The key to set.
     * @param value The value to set.
     */
    @JvmStatic
    fun put(key: String, value: String): Unit = MDC.put(key, value)


    /**
     * Puts a context key with the specified value.
     * @param pair The key/value pair.
     */
    @JvmStatic
    fun put(pair: Pair<String, String>): Unit =
        put(pair.first, pair.second)


    /**
     * Removes the specified context key.
     * @param key The key to remove.
     */
    @JvmStatic
    fun remove(key: String): Unit = MDC.remove(key)

    /**
     * Clears all keys.
     */
    @JvmStatic
    fun clear(): Unit = MDC.clear()

    /**
     * Gets the specified context key.
     * @param key The key to get.
     * @return The key value.
     */
    @JvmStatic
    fun get(key: String): String? = MDC.get(key)

    /**
     * Gets all current keys.
     * @return Map of the keys.
     */
    @JvmStatic
    fun getAll(): Map<String, String?> = MDC.getCopyOfContextMap()
}

inline fun loggingContext(body: LoggingContext.() -> Unit) {
    body(LoggingContext)
}