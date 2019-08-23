/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import kotlinx.coroutines.CompletableDeferred

internal typealias Completion = CompletableDeferred<Any?>

internal enum class MessageDirection {
    OUTBOUND,
    INBOUND
}

internal data class MessageContainer(
    val direction: MessageDirection,
    val completion: Completion,
    val msg: Message
)

internal data class Message(
    val content: MessageContent,
    val messageId: Long? = null,
    val source: NodeId? = null,
    val target: MessageTarget? = null

)

internal sealed class MessageTarget {
    data class Unicast(val targetNode: NodeId) : MessageTarget()
    data class Multicast(val nodes: Iterable<NodeId>) : MessageTarget()
    object Broadcast : MessageTarget()
}

internal sealed class MessageContent {
    data class TempStringMessage(val data: String) : MessageContent()
}