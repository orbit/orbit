/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.exception.NoAvailableNodeException
import cloud.orbit.common.logging.logger
import cloud.orbit.common.util.attempt
import cloud.orbit.common.util.randomOrNull
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.net.NodeStatus
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.RemoteInvocationTarget

class PlacementSystem(
    private val netSystem: NetSystem,
    private val addressableDirectory: AddressableDirectory
) {
    private val logger by logger()

    suspend fun locateOrPlace(rit: RemoteInvocationTarget): NetTarget =
        addressableDirectory.locate(rit).run {
            this ?: addressableDirectory.locateOrPlace(rit, selectTarget(rit))
        }

    private suspend fun selectTarget(rit: RemoteInvocationTarget): NetTarget =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {

            val allNodes = netSystem.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(rit.interfaceClass) }

            // TODO: Support multiple placement strategies
            val selectedNode = candidateNodes.randomOrNull()
            if (selectedNode != null) {
                NetTarget.Unicast(selectedNode.nodeIdentity)
            } else {
                throw NoAvailableNodeException(
                    "Could not find node capable of hosting ${rit.interfaceClass}."
                )
            }
        }

    suspend fun canHandleLocally(rit: RemoteInvocationTarget): Boolean {
        val currentLocation = addressableDirectory.locate(rit)
        return when(currentLocation) {
            is NetTarget.Unicast -> currentLocation.targetNode == netSystem.localNode.nodeIdentity
            is NetTarget.Multicast -> currentLocation.nodes.contains(netSystem.localNode.nodeIdentity)
            is NetTarget.Broadcast -> true
            else -> false
        }
    }
}