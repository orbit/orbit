/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import org.junit.Test
import kotlin.test.assertEquals

class AddressableTest {
    @Test
    fun `test addressable reference conversion`() {
        val initialRef = AddressableReference("test", Key.StringKey("testId"))
        val convertedRef = initialRef.toAddressableReferenceProto()
        val endRef = convertedRef.toAddressableReference()
        assertEquals(initialRef, endRef)
    }
}