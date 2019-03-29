/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.util

fun <E> Collection<E>.randomOrNull(): E? =
    try {
        this.random()
    } catch (nse: NoSuchElementException) {
        null
    } catch (t: Throwable) {
        throw t
    }