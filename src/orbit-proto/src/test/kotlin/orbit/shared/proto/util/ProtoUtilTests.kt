/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProtoUtilTests {
    @Test
    fun `ensure can convert to and from proto timestamp`() {
        val before = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val proto = before.toProto()
        val after = proto.toInstant()
        assertThat(after).isEqualTo(before)
    }
}