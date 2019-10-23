/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import kotlinx.coroutines.CompletableDeferred
import orbit.client.execution.ExecutionSystem
import orbit.client.net.Completion
import orbit.client.net.MessageHandler
import orbit.client.serializer.Serializer
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget
import orbit.util.di.jvm.ComponentContainer

internal class InvocationSystem(
    private val serializer: Serializer,
    private val executionSystem: ExecutionSystem,
    componentContainer: ComponentContainer
) {
    private val messageHandler by componentContainer.inject<MessageHandler>()

    suspend fun onInvocationRequest(msg: Message) {
        val content = msg.content as MessageContent.InvocationRequest
        val arguments = serializer.deserialize<Array<Any?>>(content.arguments)
        val invocation = AddressableInvocation(
            reference = content.destination,
            method = content.method,
            args = arguments
        )
        val completion: Completion = CompletableDeferred()
        executionSystem.handleInvocation(invocation, completion)
        val response = try {
            val result = completion.await()
            MessageContent.InvocationResponse(
                data = serializer.serialize(result)
            )
        } catch (t: Throwable) {
            MessageContent.Error(t.toString())
        }

        messageHandler.sendMessage(
            Message(
                messageId = msg.messageId,
                target = MessageTarget.Unicast(msg.source!!),
                content = response
            )
        )
    }

    fun onInvocationResponse(responsePayload: String, completion: Completion) {
        val result = serializer.deserialize<Any>(responsePayload)
        completion.complete(result)
    }

    fun sendInvocation(invocation: AddressableInvocation): Completion {
        val arguments = serializer.serialize(invocation.args)
        val msg = Message(
            MessageContent.InvocationRequest(
                destination = invocation.reference,
                method = invocation.method,
                arguments = arguments
            )
        )
        return messageHandler.sendMessage(msg)
    }
}