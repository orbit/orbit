/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.remoting

import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RoutingStrategy
import cloud.orbit.core.remoting.Addressable
import kotlinx.coroutines.Deferred

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
class ClassAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
@NonConcrete
interface NonConcreteAddressable : Addressable

@Lifecycle(autoActivate = true, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
interface MissingRoutingAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@ExecutionModel(ExecutionStrategy.SAFE)
interface MissingLifecycleAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true, autoDeactivate = true)
interface MissingExecutionModelAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
interface BasicValidAddressable : Addressable

interface InheritedValidAddressable : NonConcreteAddressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true, autoDeactivate = true)
@ExecutionModel(ExecutionStrategy.SAFE)
interface ValidMethodsAddressable : Addressable {
    fun getName(): Deferred<String>
}