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

        val Names = object : MeterNames {
            override val HealthCheck = "Health Check"
            override val PassingHealthChecks = "Passing Health Checks"
            override val ConnectedClients = "Connected Clients"
            override val PlacementTimer = "Placement Timer"
            override val AddressableCount = "Addressable Count"
            override val NodeCount = "Node Count"
            override val ConnectedNodes = "Connected Nodes"
            override val MessageSizes = "Message Sizes"
            override val SlowTicks = "Slow Ticks"
            override val TickTimer = "Tick Timer"
        }

        val ConnectedClients: Double get() = getMeter(Names.ConnectedClients)
        val PlacementTimer_Count: Double get() = getMeter(Names.PlacementTimer, "count")
        val PlacementTimer_TotalTime: Double get() = getMeter(Names.PlacementTimer, "total_time")
        val AddressableCount: Double get() = getMeter(Names.AddressableCount)
        val NodeCount: Double get() = getMeter(Names.NodeCount)
        val ConnectedNodes: Double get() = getMeter(Names.ConnectedNodes)
        val MessagesCount: Double get() = getMeter(Names.MessageSizes, "count")
        val MessageSizes: Double get() = getMeter(Names.MessageSizes, "total")
        val SlowTickCount: Double get() = getMeter(Names.SlowTicks)
        val TickTimer_Count: Double get() = getMeter(Names.TickTimer, "count")
        val TickTimer_Total: Double get() = getMeter(Names.TickTimer, "total_time")
    }

    interface MeterNames {
        val HealthCheck: String
        val PassingHealthChecks: String
        val ConnectedClients: String
        val PlacementTimer: String
        val AddressableCount: String
        val NodeCount: String
        val ConnectedNodes: String
        val MessageSizes: String
        val SlowTicks: String
        val TickTimer: String
    }
}