/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline.steps

import cloud.orbit.runtime.net.Message
import cloud.orbit.runtime.net.MessageContent
import cloud.orbit.runtime.pipeline.PipelineContext
import cloud.orbit.runtime.serialization.SerializationSystem

internal class SerializationStep(private val serializationSystem: SerializationSystem) : PipelineStep {
    override suspend fun onOutbound(context: PipelineContext, msg: Message) {
        val buffer = serializationSystem.serializeObject(msg.content)
        val newMsg = msg.copy(
            content = MessageContent.RawMessage(buffer)
        )
        context.nextOutbound(newMsg)
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        if (msg.content !is MessageContent.RawMessage) return

        val realContent = serializationSystem.deserializeObject<MessageContent>(msg.content.data)
        val newMsg = msg.copy(
            content = realContent
        )

        context.nextInbound(newMsg)
    }
}