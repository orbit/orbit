/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import orbit.client.actor.ActorWithNoImpl
import orbit.client.actor.ArgumentOnDeactivate
import orbit.client.actor.BasicOnDeactivate
import orbit.client.actor.ComplexDtoActor
import orbit.client.actor.GreeterActor
import orbit.client.actor.IdActor
import orbit.client.actor.IncrementActor
import orbit.client.actor.NullActor
import orbit.client.actor.TestException
import orbit.client.actor.ThrowingActor
import orbit.client.actor.TimeoutActor
import orbit.client.actor.TrackingGlobals
import orbit.client.actor.createProxy
import orbit.client.util.RemoteException
import orbit.client.util.TimeoutException
import orbit.util.misc.RNGUtils
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LifecycleTests : BaseIntegrationTest() {
    @Test
    fun `Shutting down service leaves cluster`() {

    }
}