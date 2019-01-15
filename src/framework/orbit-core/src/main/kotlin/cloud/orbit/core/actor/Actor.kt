/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.actor

import cloud.orbit.core.remoting.Addressable

/**
 * Interface marker for actors.
 */
interface Actor : Addressable

/**
 * An actor with no key.
 */
interface ActorWithNoKey : Actor

/**
 * An actor with a string key.
 */
interface ActorWithStringKey : Actor

/**
 * An actor with an int32 key.
 */
interface ActorWithInt32Key : Actor

/**
 * An actor with an int64 key.
 */
interface ActorWithInt64Key : Actor

/**
 * An actor with a guid key.
 */
interface ActorWithGuidKey : Actor