/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.remoting

import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.OnActivate
import cloud.orbit.core.annotation.OnDeactivate
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RoutingStrategy
import cloud.orbit.core.remoting.Addressable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class EmptyClassAddressable : Addressable

interface EmptyInterfaceAddressable : Addressable

@Routing(isRouted = false, forceRouting = true, routingStrategy = RoutingStrategy::class, persistentPlacement = false)
interface RoutingInterfaceAddressable : Addressable

@Routing(isRouted = false, forceRouting = true, routingStrategy = RoutingStrategy::class, persistentPlacement = false)
@Lifecycle(autoActivate = false, autoDeactivate = true)
class RoutingLifecycleClassAddressable : Addressable

@Routing(isRouted = false, forceRouting = true, routingStrategy = RoutingStrategy::class, persistentPlacement = false)
@ExecutionModel(ExecutionStrategy.SAFE)
class RoutingExecutionClassAddressable : Addressable

@Routing(isRouted = false, forceRouting = true, routingStrategy = RoutingStrategy::class, persistentPlacement = false)
@Lifecycle(autoActivate = false, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
interface FullInterfaceAddressable : Addressable

interface InvalidMethodInterfaceAddressable : FullInterfaceAddressable {
    @Suppress("UNUSED")
    fun dialTheGate(): String
}

interface ValidMethodInterfaceAddressable : FullInterfaceAddressable {
    @Suppress("UNUSED")
    fun dialTheGate(): Deferred<String>
}

class FullInheritedClassAddressable : FullInterfaceAddressable

class LifecycleEventsClassAddressable : FullInterfaceAddressable {
    @Suppress("UNUSED")
    @OnActivate
    fun onActivate() = CompletableDeferred(Unit)

    @Suppress("UNUSED")
    @OnDeactivate
    fun onDeactivate() = CompletableDeferred(Unit)
}