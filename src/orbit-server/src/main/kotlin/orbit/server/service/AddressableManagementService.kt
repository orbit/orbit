/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import orbit.server.concurrent.RuntimeScopes
import orbit.shared.proto.AddressableManagementImplBase
import orbit.shared.proto.AddressableManagementOuterClass

class AddressableManagementService(
    runtimeScopes: RuntimeScopes
) : AddressableManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    override suspend fun renewLease(request: AddressableManagementOuterClass.RenewAddressableLeaseRequestProto): AddressableManagementOuterClass.RenewAddressableLeaseResponseProto {
        return super.renewLease(request)
    }
}