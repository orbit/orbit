/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.actor

import orbit.shared.mesh.jvm.Addressable

interface GreeterActor : Addressable

class GreeterActorImpl : GreeterActor