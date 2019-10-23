/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import orbit.client.addressable.Addressable
import orbit.client.addressable.NonConcrete

/**
 * Interface marker for actors.
 */
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