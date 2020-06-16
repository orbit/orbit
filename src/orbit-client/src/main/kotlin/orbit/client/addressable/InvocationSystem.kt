/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import kotlinx.coroutines.CompletableDeferred
import orbit.client.OrbitClientConfig
import orbit.client.execution.ExecutionSystem
import orbit.client.net.ClientState
import orbit.client.net.Completion
import orbit.client.net.LocalNode
import orbit.client.net.MessageHandler
import orbit.client.serializer.Serializer
import orbit.client.util.RemoteException
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.addressable.AddressableInvocationArguments
import orbit.shared.exception.RerouteMessageException
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget
import orbit.util.di.ComponentContainer

internal class InvocationSystem(
    private val serializer: Serializer,
    private val executionSystem: ExecutionSystem,
    private val localNode: LocalNode,
    private val config: OrbitClientConfig,
    componentContainer: ComponentContainer
) {
    private val messageHandler by componentContainer.inject<MessageHandler>()

    suspend fun onInvocationRequest(msg: Message) {
        val content = msg.content as MessageContent.InvocationRequest
        val arguments = serializer.deserialize<AddressableInvocationArguments>(content.arguments)
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
            when {
                t is RerouteMessageException -> {
                    messageHandler.sendMessage(msg)
                    return
                }
                config.platformExceptions -> {
                    MessageContent.InvocationResponseError(t.toString(), serializer.serialize(t))
                }
                else -> {
                    MessageContent.Error(t.toString())
                }
            }
        }

        messageHandler.sendMessage(
            Message(
                messageId = msg.messageId,
                target = MessageTarget.Unicast(msg.source!!),
                content = response
            )
        )
    }

    fun onInvocationResponse(ir: MessageContent.InvocationResponse, completion: Completion) {
        val result = serializer.deserialize<Any>(ir.data)
        completion.complete(result)
    }

    fun onInvocationPlatformErrorResponse(ire: MessageContent.InvocationResponseError, completion: Completion) {
        val result = if (config.platformExceptions && !ire.platform.isNullOrEmpty()) {
            serializer.deserialize<Throwable>(ire.platform!!)
        } else {
            RemoteException("Exceptional response received: ${ire.description}")
        }
        completion.completeExceptionally(result)
    }

    fun sendInvocation(invocation: AddressableInvocation): Completion {
        check(localNode.status.clientState == ClientState.CONNECTED) { "The Orbit client is not connected" }

        val arguments = serializer.serialize(invocation.args)
        val msg = Message(
            MessageContent.InvocationRequest(
                destination = invocation.reference,
                method = invocation.method,
                arguments = arguments,
                reason = invocation.reason
            )
        )
        return messageHandler.sendMessage(msg)
    }
}