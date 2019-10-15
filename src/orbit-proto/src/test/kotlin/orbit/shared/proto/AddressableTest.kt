/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.AddressableReference
import org.junit.Test
import kotlin.test.assertEquals

class AddressableTest {
    @Test
    fun `test addressable reference conversion`() {
        val initialRef = AddressableReference("test", "testId")
        val convertedRef = initialRef.toAddressableReferenceProto()
        val endRef = convertedRef.toAddressableReference()
        assertEquals(initialRef, endRef)
    }
}