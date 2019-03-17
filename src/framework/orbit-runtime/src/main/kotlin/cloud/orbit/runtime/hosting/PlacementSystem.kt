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
import cloud.orbit.core.net.NodeStatus
import cloud.orbit.runtime.net.MessageTarget
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.RemoteInvocationTarget

class PlacementSystem(private val netSystem: NetSystem) {
    private val logger by logger()

    suspend fun locateOrPlace(rit: RemoteInvocationTarget): MessageTarget {
        return selectTarget(rit)
    }

    private suspend fun selectTarget(rit: RemoteInvocationTarget): MessageTarget =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {

            val allNodes = netSystem.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(rit.interfaceDefinition.interfaceClass) }

            // TODO: Support multiple placement strategies
            val selectedNode = candidateNodes.randomOrNull()
            if (selectedNode != null) {
                MessageTarget.Unicast(selectedNode.nodeIdentity)
            } else {
                throw NoAvailableNodeException(
                    "Could not find node capable of hosting ${rit.interfaceDefinition.interfaceClass}."
                )
            }
        }

    suspend fun isLocal(rit: RemoteInvocationTarget): Boolean {
        return true
    }
}