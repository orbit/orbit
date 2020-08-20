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
            override val AddressableLeaseAbandonTimer: String = "orbit_addressable_lease_abandon"
            override val AddressableLeaseRenewalTimer: String = "orbit_addressable_lease_renewal"
            override val HealthCheck = "orbit_health_checks"
            override val PassingHealthChecks = "orbit_passing_health_checks"
            override val ConnectedClients = "orbit_connected_clients"
            override val PlacementTimer = "orbit_placement_timer"
            override val AddressableCount = "orbit_addressable_count"
            override val NodeCount = "orbit_node_count"
            override val ConnectedNodes = "orbit_connected_nodes"
            override val MessageSizes = "orbit_message_sizes"
            override val SlowTicks = "orbit_slow_ticks"
            override val TickTimer = "orbit_tick_timer"
            override val RetryAttempts = "orbit_retry_attempts"
            override val RetryErrors = "orbit_retry_errors"
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
        val AddressableLeaseAbandonTimer: String
        val AddressableLeaseRenewalTimer: String
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
        val RetryAttempts: String
        val RetryErrors: String
    }
}