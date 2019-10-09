/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.NodeInfo

fun NodeInfo.toLeaseRequestResponseProto(): NodeManagementOuterClass.RequestLeaseResponseProto =
    NodeManagementOuterClass.RequestLeaseResponseProto.newBuilder()
        .setStatus(NodeManagementOuterClass.RequestLeaseResponseProto.Status.OK)
        .setInfo(toNodeInfoProto())
        .build()


fun Throwable.toLeaseRequestResponseProto(): NodeManagementOuterClass.RequestLeaseResponseProto =
    NodeManagementOuterClass.RequestLeaseResponseProto.newBuilder()
        .setStatus(NodeManagementOuterClass.RequestLeaseResponseProto.Status.ERROR)
        .setErrorDescription(toString())
        .build()