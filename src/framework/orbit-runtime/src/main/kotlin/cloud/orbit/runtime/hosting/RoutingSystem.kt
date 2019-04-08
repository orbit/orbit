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
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.AddressableDefinitionDirectory
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import java.util.concurrent.ConcurrentHashMap

internal class RoutingSystem(
    private val netSystem: NetSystem,
    private val directorySystem: DirectorySystem,
    private val definitionDirectory: AddressableDefinitionDirectory,
    private val componentProvider: ComponentProvider
) {
    private val logger by logger()
    private val routingStrategies = ConcurrentHashMap<Class<out RoutingStrategy>, RoutingStrategy>()

    // Determines where a message will be routed.
    suspend fun routeMessage(reference: AddressableReference, existingTarget: NetTarget?): NetTarget {
        val interfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(reference.interfaceClass)
        var netTarget = existingTarget

        // We check to see if routing should happen at all. If not, target should have been set externally already.
        if (interfaceDefinition.routing.isRouted) {

            // We only need to route if there is no existing target set, or routing is forced.
            if (existingTarget == null || interfaceDefinition.routing.forceRouting) {
                netTarget =
                    if (interfaceDefinition.routing.persistentPlacement) {
                        // This is placed persistently so we check if the directory already knows where.
                        // If not we select a spot.
                        directorySystem.locate(reference) ?: directorySystem.locateOrPlace(
                            reference,
                            selectTarget(interfaceDefinition)
                        )
                    } else {
                        // Placement isn't persistent so we can route it to any valid node.
                        selectTarget(interfaceDefinition)
                    }
            }
        }

        // We should have a route, if not it's an error.
        return netTarget ?: throw IllegalStateException("Failed to determine route. $interfaceDefinition")
    }

    private suspend fun selectTarget(interfaceDefinition: AddressableInterfaceDefinition): NetTarget =
    // We attempt target selection multiple times
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {
            val allNodes = netSystem.clusterNodes

            // We filter out nodes where it's impossible for them to host currently.
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(interfaceDefinition.interfaceClass) }

            // We then pass the possibilities to the routing strategy which makes a final decision,
            val strategy = routingStrategies.getOrPut(interfaceDefinition.routing.routingStrategy.java) {
                componentProvider.construct(interfaceDefinition.routing.routingStrategy.java)
            }
            val selectedTarget = strategy.selectTarget(candidateNodes)

            // Target is either found or routing failed. Another attempt may occur.
            return selectedTarget ?: throw NoAvailableNodeException(
                "Could not find node capable of hosting ${interfaceDefinition.interfaceClass}."
            )
        }

    // Determines whether this node can handle a specific addressable reference.
    suspend fun canHandleLocally(reference: AddressableReference): Boolean {
        val interfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(reference.interfaceClass)

        return if (interfaceDefinition.routing.persistentPlacement) {
            // If placement is persistent then we need to check the directory and ensure we're a valid target.
            val currentLocation = directorySystem.locate(reference)
            when (currentLocation) {
                is NetTarget.Unicast -> currentLocation.targetNode == netSystem.localNode.nodeIdentity
                is NetTarget.Multicast -> currentLocation.nodes.contains(netSystem.localNode.nodeIdentity)
                is NetTarget.Broadcast -> true
                else -> false
            }

        } else {
            // Otherwise we assume that as long as we can theoretically host it's ok.
            netSystem.localNode.nodeCapabilities.canHost(reference.interfaceClass)
        }
    }
}