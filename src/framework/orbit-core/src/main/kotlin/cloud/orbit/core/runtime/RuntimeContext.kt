/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.runtime

import cloud.orbit.common.time.Clock
import cloud.orbit.core.actor.ActorProxyFactory
import cloud.orbit.core.remoting.AddressableRegistry

/**
 * The Orbit runtime context.
 */
interface RuntimeContext {
    val clock: Clock
    val actorProxyFactory: ActorProxyFactory
    val addressableRegistry: AddressableRegistry
}