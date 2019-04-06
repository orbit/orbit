/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.net.NetTarget
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableReference
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.AddressableInterfaceClientProxy
import java.lang.reflect.Proxy

internal class ReferenceResolver(private val executionSystem: ExecutionSystem, private val netSystem: NetSystem) {
    data class RemoteAddressableReference(val reference: AddressableReference, val target: NetTarget)

    fun resolveAddressableReference(obj: Any): RemoteAddressableReference? {
        if (obj is Addressable) {
            if (obj is Proxy) {
                val handler = Proxy.getInvocationHandler(obj)
                if (handler is AddressableInterfaceClientProxy) {
                    return RemoteAddressableReference(
                        handler.reference,
                        handler.target ?: netSystem.localNode.nodeIdentity.asTarget()
                    )
                }
            }

            val realRef = executionSystem.getReferenceByInstance(obj)
            if (realRef != null) {
                return RemoteAddressableReference(realRef, netSystem.localNode.nodeIdentity.asTarget())
            }
        }

        return null
    }
}