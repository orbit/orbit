/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.net.NodeIdentity
import cloud.orbit.core.remoting.AddressableInvocation
import kotlinx.coroutines.CompletableDeferred

typealias Completion = CompletableDeferred<Any?>

enum class MessageDirection {
    OUTBOUND,
    INBOUND
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
    val target: NetTarget? = null

)

sealed class MessageContent {
    data class RequestInvocationMessage(val addressableInvocation: AddressableInvocation) : MessageContent()
    data class ResponseNormalMessage(val response: Any?) : MessageContent()
    data class ResponseErrorMessage(val error: Throwable) : MessageContent()
}
