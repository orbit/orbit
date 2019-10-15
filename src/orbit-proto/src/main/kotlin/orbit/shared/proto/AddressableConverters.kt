/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.AddressableLease
import orbit.shared.mesh.AddressableReference

fun AddressableReference.toAddressableReferenceProto() =
    Addressable.AddressableReferenceProto.newBuilder()
        .setId(id)
        .setType(type)
        .build()

fun Addressable.AddressableReferenceProto.toAddressableReference() =
    AddressableReference(
        type = type,
        id = id
    )

fun AddressableLease.toAddressableLeaseProto() =
    Addressable.AddressableLeaseProto.newBuilder()
        .setNodeId(nodeId.toNodeIdProto())
        .setReference(reference.toAddressableReferenceProto())
        .setExpiresAt(expiresAt.toTimestampProto())
        .setRenewAt(renewAt.toTimestampProto())
        .build()

fun Addressable.AddressableLeaseProto.toAddressableLease() =
    AddressableLease(
        nodeId = nodeId.toNodeId(),
        reference = reference.toAddressableReference(),
        expiresAt = expiresAt.toTimestamp(),
        renewAt = renewAt.toTimestamp()
    )