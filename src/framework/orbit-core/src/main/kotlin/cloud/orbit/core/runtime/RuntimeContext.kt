/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.runtime

import cloud.orbit.common.time.Clock
import cloud.orbit.core.actor.ActorProxyFactory

/**
 * The Orbit runtime context.
 */
interface RuntimeContext {
    val clock: Clock
    val actorProxyFactory: ActorProxyFactory
}