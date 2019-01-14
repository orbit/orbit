/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
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
    fun put(key: String, value: String) {
        MDC.put(key, value)
    }

    /**
     * Puts a context key with the specified value.
     * @param pair The key/value pair.
     */
    @JvmStatic
    fun put(pair: Pair<String, String>) {
        put(pair.first, pair.second)
    }

    /**
     * Removes the specified context key.
     * @param key The key to remove.
     */
    @JvmStatic
    fun remove(key: String) {
        MDC.remove(key)
    }

    /**
     * Clears all keys.
     */
    @JvmStatic
    fun clear() {
        MDC.clear()
    }

    /**
     * Gets the specified context key.
     * @param key The key to get.
     * @return The key value.
     */
    @JvmStatic
    fun get(key: String): String? {
        return MDC.get(key)
    }
}

inline fun loggingContext(body: LoggingContext.() -> Unit) {
    body(LoggingContext)
}