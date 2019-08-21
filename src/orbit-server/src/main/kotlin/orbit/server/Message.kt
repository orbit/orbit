/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

abstract class BaseMessage(val content: String, open val destination: BaseAddress) {

}

class Message<TAddress : BaseAddress>(content: String, override val destination: TAddress) :
    BaseMessage(content, destination) {

}
