/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.common.exception.NoAvailableNodeException
import cloud.orbit.common.logging.logger
import cloud.orbit.common.util.attempt
import cloud.orbit.core.hosting.RoutingStrategy
import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.net.NodeStatus
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.RemoteInterfaceDefinition
import cloud.orbit.runtime.remoting.RemoteInterfaceDefinitionDictionary
import cloud.orbit.runtime.remoting.RemoteInvocationTarget
import java.util.concurrent.ConcurrentHashMap

class RoutingSystem(
    private val netSystem: NetSystem,
    private val addressableDirectory: AddressableDirectory,
    private val remoteInterfaceDefinitionDictionary: RemoteInterfaceDefinitionDictionary,
    private val componentProvider: ComponentProvider
) {
    private val logger by logger()
    private val routingStrategies = ConcurrentHashMap<Class<out RoutingStrategy>, RoutingStrategy>()

    suspend fun routeMessage(rit: RemoteInvocationTarget, existingTarget: NetTarget?): NetTarget {
        val rid = remoteInterfaceDefinitionDictionary.getOrCreate(rit.interfaceClass)
        var netTarget = existingTarget

        if (rid.routing.isRouted) {
            if (existingTarget == null || rid.routing.forceRouting) {
                netTarget =
                    if (rid.routing.persistentPlacement) {
                        addressableDirectory.locate(rit).run {
                            this ?: addressableDirectory.locateOrPlace(rit, selectTarget(rid))
                        }
                    } else {
                        selectTarget(rid)
                    }
            }
        }

        return netTarget ?: throw IllegalStateException("Failed to determine route. $rid")
    }

    private suspend fun selectTarget(rid: RemoteInterfaceDefinition): NetTarget =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {
            val allNodes = netSystem.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(rid.interfaceClass) }

            val strategy = routingStrategies.getOrPut(rid.routing.routingStrategy.java) {
                componentProvider.construct(rid.routing.routingStrategy.java)
            }
            val selectedTarget = strategy.selectTarget(candidateNodes)

            return selectedTarget ?: throw NoAvailableNodeException(
                "Could not find node capable of hosting ${rid.interfaceClass}."
            )
        }

    suspend fun canHandleLocally(rit: RemoteInvocationTarget): Boolean {
        val currentLocation = addressableDirectory.locate(rit)
        return when (currentLocation) {
            is NetTarget.Unicast -> currentLocation.targetNode == netSystem.localNode.nodeIdentity
            is NetTarget.Multicast -> currentLocation.nodes.contains(netSystem.localNode.nodeIdentity)
            is NetTarget.Broadcast -> true
            else -> false
        }
    }
}