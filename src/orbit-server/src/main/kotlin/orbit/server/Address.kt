/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

abstract class BaseAddress {
    open fun capability(): Capability = Capability.None
}

open class Address<TId>(open val id: Id<TId>) : BaseAddress() {

}

