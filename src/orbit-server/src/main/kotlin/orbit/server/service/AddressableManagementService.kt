/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.service

import io.micrometer.core.instrument.Metrics
import orbit.server.concurrent.RuntimeScopes
import orbit.server.mesh.AddressableManager
import orbit.server.service.ServerAuthInterceptor.Keys.NODE_ID
import orbit.shared.proto.AddressableManagementImplBase
import orbit.shared.proto.AddressableManagementOuterClass
import orbit.shared.proto.toAddressableLeaseResponseProto
import orbit.shared.proto.toAddressableReference
import orbit.util.instrumentation.recordSuspended

class AddressableManagementService(
    private val addressableManager: AddressableManager,
    runtimeScopes: RuntimeScopes
) : AddressableManagementImplBase(runtimeScopes.ioScope.coroutineContext) {
    private val addressableLeaseRenewalTimer = Metrics.timer(Meters.Names.AddressableLeaseRenewalTimer)
    private val addressableLeaseAbandonTimer = Metrics.timer(Meters.Names.AddressableLeaseAbandonTimer)

    override suspend fun renewLease(request: AddressableManagementOuterClass.RenewAddressableLeaseRequestProto)
            : AddressableManagementOuterClass.RenewAddressableLeaseResponseProto =
        addressableLeaseRenewalTimer.recordSuspended {
            try {
                addressableManager.renewLease(
                    request.reference.toAddressableReference(),
                    NODE_ID.get()
                ).toAddressableLeaseResponseProto()
            } catch (t: Throwable) {
                t.toAddressableLeaseResponseProto()
            }
        }

    override suspend fun abandonLease(request: AddressableManagementOuterClass.AbandonAddressableLeaseRequestProto)
            : AddressableManagementOuterClass.AbandonAddressableLeaseResponseProto =
        addressableLeaseAbandonTimer.recordSuspended {
            val nodeId = NODE_ID.get()
            val reference = request.reference.toAddressableReference()
            val result = try {
                addressableManager.abandonLease(reference, nodeId)
            } catch (t: Throwable) {
                false
            }

            AddressableManagementOuterClass.AbandonAddressableLeaseResponseProto.newBuilder()
                .setAbandoned(result)
                .build()
        }
}
