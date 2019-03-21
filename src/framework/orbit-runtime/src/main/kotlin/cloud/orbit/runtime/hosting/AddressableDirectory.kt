/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.runtime.net.MessageTarget
import cloud.orbit.runtime.remoting.RemoteInvocationTarget

interface AddressableDirectory {
    suspend fun locate(remoteInvocationTarget: RemoteInvocationTarget) : MessageTarget?
    suspend fun locateOrPlace(remoteInvocationTarget: RemoteInvocationTarget, messageTarget: MessageTarget): MessageTarget
}