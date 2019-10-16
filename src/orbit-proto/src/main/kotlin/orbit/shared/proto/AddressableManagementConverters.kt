/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.addressable.AddressableLease

fun AddressableLease.toAddressableLeaseResponseProto(): AddressableManagementOuterClass.RenewAddressableLeaseResponseProto =
    AddressableManagementOuterClass.RenewAddressableLeaseResponseProto.newBuilder()
        .setStatus(AddressableManagementOuterClass.RenewAddressableLeaseResponseProto.Status.OK)
        .setLease(toAddressableLeaseProto())
        .build()

fun Throwable.toAddressableLeaseResponseProto(): AddressableManagementOuterClass.RenewAddressableLeaseResponseProto =
    AddressableManagementOuterClass.RenewAddressableLeaseResponseProto.newBuilder()
        .setStatus(AddressableManagementOuterClass.RenewAddressableLeaseResponseProto.Status.ERROR)
        .setErrorDescription(toString())
        .build()