/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.AddressableManager
import orbit.server.service.ServerAuthInterceptor.Keys.NODE_ID
import orbit.shared.proto.AddressableManagementImplBase
import orbit.shared.proto.AddressableManagementOuterClass
import orbit.shared.proto.toAddressableLeaseResponseProto
import orbit.shared.proto.toAddressableReference

class AddressableManagementService(
    private val addressableManager: AddressableManager,
    runtimeScopes: RuntimeScopes
) : AddressableManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    override suspend fun renewLease(request: AddressableManagementOuterClass.RenewAddressableLeaseRequestProto): AddressableManagementOuterClass.RenewAddressableLeaseResponseProto {
        return try {
            val nodeId = NODE_ID.get()
            val reference = request.reference.toAddressableReference()
            addressableManager.renewLease(reference, nodeId).toAddressableLeaseResponseProto()
        } catch (t: Throwable) {
            t.toAddressableLeaseResponseProto()
        }
    }
}