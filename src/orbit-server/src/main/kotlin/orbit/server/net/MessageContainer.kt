/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.shared.net.Message

class MessageContainer(
    val direction: MessageDirection,
    val message: Message
)

enum class MessageDirection {
    INBOUND,
    OUTBOUND;
}