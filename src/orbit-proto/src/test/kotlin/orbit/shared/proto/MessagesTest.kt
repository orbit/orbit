/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.net.MessageContent
import orbit.shared.remoting.AddressableReference
import org.junit.Test
import kotlin.test.assertEquals

class MessagesTest {
    @Test
    fun `test invocation request message content conversion`() {
        val initialRef = MessageContent.InvocationRequest(
            "test",
            AddressableReference("refTye", "refId")
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