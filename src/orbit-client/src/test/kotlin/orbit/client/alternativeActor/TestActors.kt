/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.alternativeActor

import orbit.client.actor.AbstractActor
import orbit.client.actor.ActorWithStringKey

interface TestActor : ActorWithStringKey {
}

class TestActorImpl : TestActor, AbstractActor() {

}