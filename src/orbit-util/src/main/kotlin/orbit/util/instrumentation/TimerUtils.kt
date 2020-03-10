/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.instrumentation

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer

suspend fun <R> Timer.recordSuspended(f: suspend () -> R) : R {
    var sample: Timer.Sample? = null
    try {
        sample = Timer.start(Metrics.globalRegistry.registries.first().config().clock())
        return f()
    } finally {
        sample!!.stop(this)
    }
}
