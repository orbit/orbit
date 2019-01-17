/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.pipeline

import cloud.orbit.runtime.concurrent.SupervisorScope
import cloud.orbit.runtime.remoting.RemoteInvocation
import kotlinx.coroutines.launch

class PipelineManager(
    private val supervisorScope: SupervisorScope
) {
    fun writeInvocation(remoteInvocation: RemoteInvocation) {
        supervisorScope.launch {
            remoteInvocation.completion.complete("Joe rocks")
        }
    }
}