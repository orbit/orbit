/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.net.InvocationReason
import orbit.shared.net.MessageContent
import org.junit.Test
import kotlin.test.assertEquals

class MessagesTest {
    @Test
    fun `test invocation request message content conversion`() {
        val initialRef = MessageContent.InvocationRequest(
            method = "test",
            arguments = "[]",
            destination = AddressableReference("refType", Key.StringKey("refId")),
            reason = InvocationReason.rerouted
        )
        val convertedRef = initialRef.toMessageContentProto()
        val endRef = convertedRef.toMessageContent()
        assertEquals(initialRef, endRef)
    }

    @Test
    fun `test invocation response message content conversion`() {
        val initialRef = MessageContent.InvocationResponse("test")
        val convertedRef = initialRef.toMessageContentProto()
        val endRef = convertedRef.toMessageContent()
        assertEquals(initialRef, endRef)
    }
}