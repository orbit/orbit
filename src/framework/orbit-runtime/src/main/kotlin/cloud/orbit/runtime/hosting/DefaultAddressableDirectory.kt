/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.runtime.net.MessageTarget
import cloud.orbit.runtime.remoting.RemoteInvocationTarget
import java.util.concurrent.ConcurrentHashMap

class DefaultAddressableDirectory : AddressableDirectory {
    private val concurrentHashMap = ConcurrentHashMap<RemoteInvocationTarget, MessageTarget>()

    override suspend fun locate(remoteInvocationTarget: RemoteInvocationTarget): MessageTarget? =
        concurrentHashMap[remoteInvocationTarget]

    override suspend fun locateOrPlace(
        remoteInvocationTarget: RemoteInvocationTarget,
        messageTarget: MessageTarget
    ): MessageTarget =
        concurrentHashMap.getOrPut(remoteInvocationTarget) {
            messageTarget
        }
}