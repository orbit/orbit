/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import io.grpc.Context

fun <T> Context.Key<T>.getOrNull() = runCatching { get() }.getOrNull()