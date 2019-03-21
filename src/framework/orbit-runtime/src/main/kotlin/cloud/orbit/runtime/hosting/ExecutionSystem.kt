/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.runtime.net.Completion
import cloud.orbit.runtime.remoting.RemoteInvocation

class ExecutionSystem {
    suspend fun handleInvocation(remoteInvocation: RemoteInvocation, completion: Completion) {
        completion.complete("Hello Orbit!")
    }
}