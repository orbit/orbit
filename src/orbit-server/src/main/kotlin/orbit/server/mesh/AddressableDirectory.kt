/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.mesh

import orbit.shared.mesh.AddressableLease
import orbit.shared.mesh.AddressableReference
import orbit.util.concurrent.AsyncMap

interface AddressableDirectory : AsyncMap<AddressableReference, AddressableLease>