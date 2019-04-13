/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.RemoteAddressableReference
import cloud.orbit.runtime.net.NetSystem
import cloud.orbit.runtime.remoting.AddressableInterfaceClientProxy
import java.lang.reflect.Proxy

internal class ReferenceResolver(private val executionSystem: ExecutionSystem, private val netSystem: NetSystem) {
    fun resolveAddressableReference(obj: Any): RemoteAddressableReference? {
        // First we check if this is an addressable at all
        if (obj is Addressable) {
            // The easiest case is if it's already a proxy, we basically just clone it.
            if (obj is Proxy) {
                val handler = Proxy.getInvocationHandler(obj)
                if (handler is AddressableInterfaceClientProxy) {
                    return RemoteAddressableReference(
                        handler.reference,
                        // We copy the address if we already have it, otherwise we assume it's local.
                        // Routing will override the target if appropriate.
                        handler.target ?: netSystem.localNode.nodeIdentity.asTarget()
                    )
                }
            }

            // If it's not a proxy we check to see if the actual object reference is being tracked by execution.
            executionSystem.getReferenceByInstance(obj)?.also {
                return RemoteAddressableReference(it, netSystem.localNode.nodeIdentity.asTarget())
            }
        }

        return null
    }
}