/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.test

import kotlinx.coroutines.runBlocking

actual fun <T> runTest(block: suspend () -> T) = runBlocking { block() }
