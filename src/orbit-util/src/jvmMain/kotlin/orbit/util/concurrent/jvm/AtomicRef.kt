/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.concurrent.jvm

import java.util.concurrent.atomic.AtomicReference

/**
 * Atomically updates a reference with the logic provided in the code block.
 * The code block should be free of side-effects as it may be run multiple times.
 * The initial value of the reference is passed to the code block and should be used
 * for manipulation.
 *
 * @param block The code block to run.
 * @return The reference value after the update has been applied.
 */
inline fun <T> AtomicReference<T>.atomicSet(crossinline block: (T) -> T): T {
    do {
        val initialValue = this.get()
        val newValue = block(initialValue)
    } while (!this.compareAndSet(initialValue, newValue))
    return this.get()
}