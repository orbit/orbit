/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.addressable.AddressableLease
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.addressable.NamespacedAddressableReference

fun AddressableReference.toAddressableReferenceProto() =
    Addressable.AddressableReferenceProto.newBuilder()
        .setType(type)
        .setKey(key.toAddressableKeyProto())
        .build()

fun Addressable.AddressableReferenceProto.toAddressableReference() =
    AddressableReference(
        type = type,
        key = key.toAddressableKey()
    )

fun Key.toAddressableKeyProto() = Addressable.AddressableKeyProto.newBuilder().let {
    when (this) {
        is Key.Int32Key -> it.setInt32Key(key)
        is Key.Int64Key -> it.setInt64Key(key)
        is Key.StringKey -> it.setStringKey(key)
        is Key.NoKey -> it.setNoKey(true)
    }
}.build()

fun Addressable.AddressableKeyProto.toAddressableKey(): Key =
    when (this.keyCase.number) {
        Addressable.AddressableKeyProto.INT32KEY_FIELD_NUMBER -> Key.Int32Key(int32Key)
        Addressable.AddressableKeyProto.INT64KEY_FIELD_NUMBER -> Key.Int64Key(int64Key)
        Addressable.AddressableKeyProto.STRINGKEY_FIELD_NUMBER -> Key.StringKey(stringKey)
        Addressable.AddressableKeyProto.NOKEY_FIELD_NUMBER -> Key.NoKey
        else -> error("Invalid key type")
    }

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

fun NamespacedAddressableReference.toNamespacedAddressableReferenceProto() =
    Addressable.NamespacedAddressableReferenceProto.newBuilder()
        .setNamespace(namespace)
        .setAddressableReference(addressableReference.toAddressableReferenceProto())
        .build()

fun Addressable.NamespacedAddressableReferenceProto.toNamespacedAddressableReference() =
    NamespacedAddressableReference(
        namespace = namespace,
        addressableReference = this.addressableReference.toAddressableReference()
    )
