/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import orbit.client.serializer.Serializer
import orbit.shared.addressable.AddressableReference
import orbit.shared.net.Message
import orbit.shared.net.MessageContent

internal class InvocationSystem(
    private val serializer: Serializer
) {
    fun onInvocationRequest(msg: Message) {
        val content = msg.content as MessageContent.InvocationRequest
        val args = serializer.deserializeArgs(content.data)

    }

    fun generateInvokeMessage(reference: AddressableReference, args: Array<Any?>): Message {
        val serializedArgs = serializer.serializeArgs(args)
        return Message(
            MessageContent.InvocationRequest(
                serializedArgs,
                reference
            )
        )
    }
}