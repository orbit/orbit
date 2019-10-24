/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

fun <E> Collection<E>.randomOrNull(): E? =
    runCatching {
        this.random()
    }.getOrNull()