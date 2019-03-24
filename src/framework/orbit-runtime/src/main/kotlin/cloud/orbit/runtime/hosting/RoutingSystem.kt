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
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinition
import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import java.util.concurrent.ConcurrentHashMap

class RoutingSystem(
    private val netSystem: NetSystem,
    private val addressableDirectory: AddressableDirectory,
    private val interfaceDefinitionDictionary: AddressableInterfaceDefinitionDictionary,
    private val componentProvider: ComponentProvider
) {
    private val logger by logger()
    private val routingStrategies = ConcurrentHashMap<Class<out RoutingStrategy>, RoutingStrategy>()

    suspend fun routeMessage(addressableReference: AddressableReference, existingTarget: NetTarget?): NetTarget {
        val addressableInterfaceDefinition =
            interfaceDefinitionDictionary.getOrCreate(addressableReference.interfaceClass)
        var netTarget = existingTarget

        if (addressableInterfaceDefinition.routing.isRouted) {
            if (existingTarget == null || addressableInterfaceDefinition.routing.forceRouting) {
                netTarget =
                    if (addressableInterfaceDefinition.routing.persistentPlacement) {
                        addressableDirectory.locate(addressableReference).run {
                            this ?: addressableDirectory.locateOrPlace(
                                addressableReference,
                                selectTarget(addressableInterfaceDefinition)
                            )
                        }
                    } else {
                        selectTarget(addressableInterfaceDefinition)
                    }
            }
        }

        return netTarget ?: throw IllegalStateException("Failed to determine route. $addressableInterfaceDefinition")
    }

    private suspend fun selectTarget(addressableInterfaceDefinition: AddressableInterfaceDefinition): NetTarget =
        attempt(
            maxAttempts = 5,
            initialDelay = 1000,
            logger = logger
        ) {
            val allNodes = netSystem.clusterNodes
            val candidateNodes = allNodes
                .filter { it.nodeStatus == NodeStatus.RUNNING }
                .filter { it.nodeCapabilities.canHost(addressableInterfaceDefinition.interfaceClass) }

            val strategy = routingStrategies.getOrPut(addressableInterfaceDefinition.routing.routingStrategy.java) {
                componentProvider.construct(addressableInterfaceDefinition.routing.routingStrategy.java)
            }
            val selectedTarget = strategy.selectTarget(candidateNodes)

            return selectedTarget ?: throw NoAvailableNodeException(
                "Could not find node capable of hosting ${addressableInterfaceDefinition.interfaceClass}."
            )
        }

    suspend fun canHandleLocally(addressableReference: AddressableReference): Boolean {
        val currentLocation = addressableDirectory.locate(addressableReference)
        return when (currentLocation) {
            is NetTarget.Unicast -> currentLocation.targetNode == netSystem.localNode.nodeIdentity
            is NetTarget.Multicast -> currentLocation.nodes.contains(netSystem.localNode.nodeIdentity)
            is NetTarget.Broadcast -> true
            else -> false
        }
    }
}