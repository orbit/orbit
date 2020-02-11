/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.net

import orbit.shared.addressable.AddressableReference
import orbit.shared.mesh.NodeId
import orbit.shared.router.Route

data class Message(
    val content: MessageContent,
    val messageId: Long? = null,
    val source: NodeId? = null,
    val target: MessageTarget? = null
)

sealed class MessageTarget {
    data class Unicast(val targetNode: NodeId) : MessageTarget()
    data class RoutedUnicast(val route: Route) : MessageTarget()
}

sealed class MessageContent {
    data class Error(val description: String?) : MessageContent()
    object ConnectionInfoRequest : MessageContent()
    data class ConnectionInfoResponse(val nodeId: NodeId) : MessageContent()
    data class InvocationRequest(val destination: AddressableReference, val method: String, val arguments: String) :
        MessageContent()

    data class InvocationResponse(val data: String) : MessageContent()
    data class InvocationResponseError(val description: String?, val platform: String?) : MessageContent()
}