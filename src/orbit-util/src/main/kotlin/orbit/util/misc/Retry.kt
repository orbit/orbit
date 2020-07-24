/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import kotlinx.coroutines.time.delay
import java.time.Duration

suspend fun <T> retry(
    retryDelay: Duration = Duration.ZERO,
    attempts: Int = Int.MAX_VALUE,
    action: suspend () -> T
): T? {
    var remaining = attempts
    while (remaining-- > 0) {
        try {
            return action()
        } catch (t: Throwable) {
            delay(retryDelay)
        }
    }
    throw RetriesExceededException("Failed operation after ${attempts} attempts")
}

class RetriesExceededException(msg: String) : Exception(msg)


