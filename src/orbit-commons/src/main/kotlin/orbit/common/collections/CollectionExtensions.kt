/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

/**
 * Gets a random element or null if no elements are available.
 * @return A random element or null.
 */
fun <E> Collection<E>.randomOrNull(): E? =
    try {
        this.random()
    } catch (nse: NoSuchElementException) {
        null
    }