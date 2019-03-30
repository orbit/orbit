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

    suspend fun routeMessage(reference: AddressableReference, existingTarget: NetTarget?): NetTarget {
        val interfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(reference.interfaceClass)
        var netTarget = existingTarget

        if (interfaceDefinition.routing.isRouted) {
            if (existingTarget == null || interfaceDefinition.routing.forceRouting) {
                netTarget =
                    if (interfaceDefinition.routing.persistentPlacement) {
                        directorySystem.locate(reference).run {
                            this ?: directorySystem.locateOrPlace(
                                reference,
                                selectTarget(interfaceDefinition)
                            )
                        }
                    } else {
                        selectTarget(interfaceDefinition)
                    }
            }
        }

        return netTarget ?: throw IllegalStateException("Failed to determine route. $interfaceDefinition")
    }

    private suspend fun selectTarget(interfaceDefinition: AddressableInterfaceDefinition): NetTarget =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {
            val allNodes = netSystem.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(interfaceDefinition.interfaceClass) }

            val strategy = routingStrategies.getOrPut(interfaceDefinition.routing.routingStrategy.java) {
                componentProvider.construct(interfaceDefinition.routing.routingStrategy.java)
            }
            val selectedTarget = strategy.selectTarget(candidateNodes)

            return selectedTarget ?: throw NoAvailableNodeException(
                "Could not find node capable of hosting ${interfaceDefinition.interfaceClass}."
            )
        }

    suspend fun canHandleLocally(reference: AddressableReference): Boolean {
        val interfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(reference.interfaceClass)

        return if (interfaceDefinition.routing.persistentPlacement) {
            val currentLocation = directorySystem.locate(reference)
            when (currentLocation) {
                is NetTarget.Unicast -> currentLocation.targetNode == netSystem.localNode.nodeIdentity
                is NetTarget.Multicast -> currentLocation.nodes.contains(netSystem.localNode.nodeIdentity)
                is NetTarget.Broadcast -> true
                else -> false
            }
        } else {
            true
        }
    }
}