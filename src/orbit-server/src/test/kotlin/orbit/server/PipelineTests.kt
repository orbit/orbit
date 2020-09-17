/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

//import orbit.server.pipeline.step.RetryStep
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import orbit.server.pipeline.PipelineContext
import orbit.server.pipeline.step.RoutingStep
import orbit.server.router.Router
import orbit.shared.mesh.NodeId
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget
import orbit.shared.router.Route
import org.junit.Test

class PipelineTests : BaseServerTest() {

    @Test
    fun `When a node isn't present in cluster, message is sent to pipeline with attempts incremented`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode)
            )

            val router = mock<Router> {
                onBlocking { findRoute(testNode, null) } doReturn (Route(emptyList()))
            }

            val routingStep = RoutingStep(router, OrbitServerConfig())

            val context = mock<PipelineContext>()
            routingStep.onOutbound(context, message)

            verify(context, never()).next(any())
            verify(context).pushNew(message.copy(attempts = 1))
        }
    }

    @Test
    fun `When a node is present in cluster, message is sent to next step`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val route = Route(listOf(testNode))
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode)
            )

            val router = mock<Router> {
                onBlocking { findRoute(testNode, null) } doReturn route
            }

            val routingStep = RoutingStep(router, OrbitServerConfig())

            val context = mock<PipelineContext>()
            routingStep.onOutbound(context, message)

            verify(context).next(message.copy(target = MessageTarget.RoutedUnicast(route)))
            verify(context, never()).pushNew(any(), anyOrNull())
        }
    }

    @Test
    fun `When a message exceeds retry attempts, error message is sent to pipeline`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val sourceNode = NodeId("source", "test")
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode),
                source = sourceNode,
                attempts = 11
            )

            val router = mock<Router> {
                onBlocking { findRoute(testNode, null) } doReturn (Route(emptyList()))
            }

            val routingStep = RoutingStep(router, OrbitServerConfig())

            val context = mock<PipelineContext>()
            routingStep.onOutbound(context, message)

            verify(context, never()).next(any())
            verify(context).pushNew(argThat { content is MessageContent.Error }, anyOrNull())
        }
    }

    @Test
    fun `When an error message exceeds retry attempts, the message is discarded`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val sourceNode = NodeId("source", "test")
            val message = Message(
                mock<MessageContent.Error>(),
                target = MessageTarget.Unicast(testNode),
                source = sourceNode,
                attempts = 11
            )

            val router = mock<Router> {
                onBlocking { findRoute(testNode, null) } doReturn (Route(emptyList()))
            }

            val routingStep = RoutingStep(router, OrbitServerConfig())

            val context = mock<PipelineContext>()
            routingStep.onOutbound(context, message)

            verify(context, never()).next(any())
            verify(context, never()).pushNew(any(), anyOrNull())
        }
    }
}
