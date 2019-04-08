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
       serializationSystem.serializeObject(msg).let {
            msg.copy(
                content = MessageContent.RawMessage(it)
            )
        }.also {
            context.nextOutbound(it)
        }
    }

    override suspend fun onInbound(context: PipelineContext, msg: Message) {
        if (msg.content !is MessageContent.RawMessage) {
            throw IllegalArgumentException(
                "Expected ${MessageContent.RawMessage::class.java.simpleName}, " +
                        "instead got ${msg.content::class.java.simpleName}."
            )
        }

        serializationSystem.deserializeObject<Message>(msg.content.data).also {
            context.nextInbound(it)
        }
    }
}