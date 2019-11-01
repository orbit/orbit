/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.LocalNodeInfo

class HealthCheckList(
    private val localNodeInfo: LocalNodeInfo,
    private val addressableDirectory: AddressableDirectory
) {
    val checks = listOf(
        this.localNodeInfo,
        this.addressableDirectory
    )

    fun getChecks(): Iterable<HealthCheck> {
        return checks
    }
}