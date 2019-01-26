/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

@file:JvmName("Logging")

package cloud.orbit.common.logging

import cloud.orbit.common.logging.impl.OrbitDefaultLogger
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory as SLF4JLoggerFactory


/**
 * Creates a [Logger] with the specified name.
 * @param logName The name.
 * @return The new [Logger].
 */
fun getLogger(logName: String): Logger =
    OrbitDefaultLogger(SLF4JLoggerFactory.getLogger(logName))

/**
 * Creates a [Logger] for the specified [Class].
 * @param clazz The [Class].
 * @return The new [Logger].
 */
fun getLogger(clazz: Class<*>): Logger =
    OrbitDefaultLogger(SLF4JLoggerFactory.getLogger(clazz))

/**
 * Creates a [Logger] for the specified [KClass].
 * @param kClazz The [KClass].
 * @return The new [Logger].
 */
fun getLogger(kClazz: KClass<*>): Logger = getLogger(kClazz.java)

/**
 * Creates a [Logger] for the specified type.
 * @param T The type.
 * @return The new [Logger].
 */
inline fun <reified T> getLogger(): Logger = getLogger(T::class)

/**
 * Creates a delegate [Logger] for the specified type.
 * @param T The type.
 * @return The new [Logger].
 */
inline fun <reified T> T.logger(): Lazy<Logger> {
    return lazy {
        getLogger<T>()
    }
}