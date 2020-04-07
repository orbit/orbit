/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import io.micrometer.core.instrument.Metrics

open class Meters {

    companion object {
        private fun getMeter(name: String, statistic: String? = null): Double {
            return Metrics.globalRegistry.meters.first { m -> m.id.name == name }.measure()
                .first { m -> statistic == null || statistic.equals(m.statistic.name, true) }.value
        }

        val ConnectedClients: Double get() = getMeter("Connected Clients")
        val PlacementTimer_Count: Double get() = getMeter("Placement Timer", "count")
        val PlacementTimer_TotalTime: Double get() = getMeter("Placement Timer", "total_time")
        val AddressableCount: Double get() = getMeter("Addressable Count")
        val NodeCount: Double get() = getMeter("Node Count")
        val ConnectedNodes: Double get() = getMeter("Connected Nodes")
        val MessagesCount: Double get() = getMeter("Message Sizes", "count")
        val MessageSizes: Double get() = getMeter("Message Sizes", "total")
        val SlowTickCount: Double get() = getMeter("Slow Ticks")
        val TickTimer_Count: Double get() = getMeter("Tick Timer", "count")
        val TickTimer_Total: Double get() = getMeter("Tick Timer", "total_time")
    }
}