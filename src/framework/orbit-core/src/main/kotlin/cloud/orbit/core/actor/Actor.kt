/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.actor

import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.RandomRouting
import cloud.orbit.core.remoting.Addressable

/**
 * Interface marker for actors.
 */
@Routing(
    isRouted = true,
    forceRouting = true,
    routingStrategy = RandomRouting::class,
    persistentPlacement = true
)
@Lifecycle(
    autoActivate = true
)
@NonConcrete
interface Actor : Addressable

/**
 * An actor with no key.
 */
@NonConcrete
interface ActorWithNoKey : Actor

/**
 * An actor with a string key.
 */
@NonConcrete
interface ActorWithStringKey : Actor

/**
 * An actor with an int32 key.
 */
@NonConcrete
interface ActorWithInt32Key : Actor

/**
 * An actor with an int64 key.
 */
@NonConcrete
interface ActorWithInt64Key : Actor

/**
 * An actor with a guid key.
 */
@NonConcrete
interface ActorWithGuidKey : Actor