/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.actor

import cloud.orbit.core.actor.Actor
import cloud.orbit.core.actor.ActorProxyFactory
import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.AddressableRegistry

internal class ActorProxyFactoryImpl(
    private val addressableRegistry: AddressableRegistry
) :
    ActorProxyFactory {
    override fun <T : Actor> getReferenceInternal(grainType: Class<T>, grainKey: Key): T =
        addressableRegistry.getReference(grainType, grainKey)
}