/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.plugin.spring.misc

import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class DemoSingleton {
    private val counter = AtomicInteger()

    fun countUp() = counter.incrementAndGet()
}