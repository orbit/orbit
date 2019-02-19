/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.net

import cloud.orbit.runtime.remoting.RemoteInvocation
import kotlinx.coroutines.CompletableDeferred

sealed class DirectionalMessage {
    abstract val content: MessageContent

    data class OutboundMessage(override val content: MessageContent) : DirectionalMessage()
    data class InboundMessage(override val content: MessageContent) : DirectionalMessage()

}

sealed class MessageContent {
    abstract val completion: CompletableDeferred<Any?>

    data class InvocationRequest(
        override val completion: CompletableDeferred<Any?>,
        val remoteInvocation: RemoteInvocation
    ) : MessageContent()

}

