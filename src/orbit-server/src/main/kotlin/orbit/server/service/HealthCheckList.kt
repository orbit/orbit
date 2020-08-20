/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.OrbitServer
import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.LocalNodeInfo
import orbit.server.mesh.NodeDirectory

class HealthCheckList(
    private val server: OrbitServer,
    private val localNodeInfo: LocalNodeInfo,
    private val addressableDirectory: AddressableDirectory,
    private val nodeDirectory: NodeDirectory
) {
    val checks = listOf(
        this.server,
        this.localNodeInfo,
        this.addressableDirectory,
        this.nodeDirectory
    )

    fun getChecks(): Iterable<HealthCheck> {
        return checks
    }
}