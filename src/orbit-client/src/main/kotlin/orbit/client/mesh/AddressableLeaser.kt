/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.mesh

import kotlinx.coroutines.guava.await
import orbit.client.net.GrpcClient
import orbit.shared.addressable.AddressableReference
import orbit.shared.proto.AddressableManagementGrpc
import orbit.shared.proto.AddressableManagementOuterClass
import orbit.shared.proto.toAddressableLease
import orbit.shared.proto.toAddressableReferenceProto

internal class AddressableLeaser(grpcClient: GrpcClient) {
    private val addressableManagementStub = AddressableManagementGrpc.newFutureStub(grpcClient.channel)

    // TODO - If renewing the lease fails, this throws an obscure error (java.lang.IllegalStateException: Invalid key type)
    suspend fun renewLease(reference: AddressableReference) =
        addressableManagementStub.renewLease(
            AddressableManagementOuterClass.RenewAddressableLeaseRequestProto.newBuilder()
                .setReference(reference.toAddressableReferenceProto())
                .build()
        ).await()?.lease?.toAddressableLease()

    suspend fun abandonLease(reference: AddressableReference) =
        addressableManagementStub.abandonLease(
            AddressableManagementOuterClass.AbandonAddressableLeaseRequestProto.newBuilder()
                .setReference(reference.toAddressableReferenceProto())
                .build()
        ).await()?.abandoned ?: false
}