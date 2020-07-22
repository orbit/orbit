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
    action: suspend () -> T
): T? {
    while (true) {
        try {
            return action()
        } catch (t: Throwable) {
            delay(retryDelay)
        }
    }
}


