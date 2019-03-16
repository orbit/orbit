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
import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.core.net.NodeStatus
import cloud.orbit.runtime.net.NetManager
import cloud.orbit.runtime.remoting.RemoteInvocationTarget

class HostingManager(private val netManager: NetManager) {
    private val logger by logger()

    suspend fun locateOrPlace(rit: RemoteInvocationTarget): NodeIdentity  {
        return selectNode(rit)
    }

    private suspend fun selectNode(rit: RemoteInvocationTarget): NodeIdentity =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {

            val allNodes = netManager.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(rit.interfaceDefinition.interfaceClass) }

            // TODO: Support multiple placement strategies
            val selectedNode = candidateNodes.randomOrNull()

            selectedNode?.nodeIdentity
                ?: throw NoAvailableNodeException(
                    "Could not find node capable of hosting ${rit.interfaceDefinition.interfaceClass}."
                )
        }

    suspend fun isLocal(rit: RemoteInvocationTarget): Boolean {
        return true
    }
}