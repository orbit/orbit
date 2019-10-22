/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.shared.net.Message
import orbit.shared.router.Route

interface MessageSender {
    suspend fun sendMessage(message: Message, route: Route? = null)
}