/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.addressable.AddressableReference

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