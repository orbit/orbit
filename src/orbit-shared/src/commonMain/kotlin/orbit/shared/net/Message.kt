/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.net

import orbit.shared.addressable.AddressableReference

sealed class Message {
    data class InvocationRequest(val data: String, val destination: AddressableReference) : Message()
    data class InvocationResponse(val data: String) : Message()
    data class InvocationError(val status: Status, val message: String) : Message() {
        enum class Status {
            UNKNOWN,
            UNAUTHENTICATED,
            UNAUTHORIZED,
            UNSENT
        }
    }
}