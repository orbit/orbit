/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.remoting

import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.RoutingStrategy
import cloud.orbit.core.remoting.Addressable
import kotlinx.coroutines.Deferred

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true)
class ClassAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true)
@NonConcrete
interface NonConcreteAddressable : Addressable

@Lifecycle(autoActivate = true)
interface MissingRoutingAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
interface MissingLifecycleAddressable : Addressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true)
interface BasicValidAddressable : Addressable

interface InheritedValidAddressable : NonConcreteAddressable

@Routing(isRouted = true, persistentPlacement = true, forceRouting = true, routingStrategy = RoutingStrategy::class)
@Lifecycle(autoActivate = true)
interface ValidMethodsAddressable : Addressable {
    fun getName() : Deferred<String>
}