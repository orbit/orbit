/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.runtime.remoting.RemoteInvocation
import kotlinx.coroutines.CompletableDeferred

typealias Completion = CompletableDeferred<Any?>

enum class MessageDirection {
    OUTBOUND,
    INBOUND
}

sealed class MessageTarget {
    data class Unicast(val targetNode: NodeIdentity) : MessageTarget()
    data class Multicast(val nodes: Iterable<NodeIdentity>) : MessageTarget() {
        constructor(vararg nodes: NodeIdentity) : this(nodes.asIterable())
    }
    object Broadcast: MessageTarget()
}

data class MessageContainer(
    val direction: MessageDirection,
    val completion: Completion,
    val msg: Message
)

enum class MessageType {
    INVOCATION_REQUEST,
    INVOCATION_RESPONSE_NORMAL,
    INVOCATION_RESPONSE_ERROR
}

data class Message(
    val messageType: MessageType,
    val messageId: Long? = null,
    val source: NodeIdentity? = null,
    val target: MessageTarget? = null,
    val remoteInvocation: RemoteInvocation? = null,
    val normalResponse: Any? = null,
    val errorResponse: Throwable? = null

)

