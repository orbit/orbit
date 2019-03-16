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

data class Message(
    val content: MessageContent,
    val messageId: Long? = null,
    val source: NodeIdentity? = null,
    val target: MessageTarget? = null

)

sealed class MessageContent {
    data class RequestInvocationMessage(val remoteInvocation: RemoteInvocation) : MessageContent()
    data class ResponseNormalMessage(val response: Any?) : MessageContent()
    data class ResponseErrorMessage(val error: Throwable) : MessageContent()
}
