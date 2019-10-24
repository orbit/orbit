/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.util.time.Timestamp
import org.junit.Test
import kotlin.test.assertEquals

class TimestampTest {
    @Test
    fun `test timestamp conversion`() {
        val initialRef = Timestamp.now()
        val convertedRef = initialRef.toTimestampProto()
        val endRef = convertedRef.toTimestamp()
        assertEquals(initialRef, endRef)
    }
}