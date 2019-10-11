/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.proto

import orbit.shared.mesh.NodeInfo

fun NodeInfo.toNodeLeaseRequestResponseProto(): NodeManagementOuterClass.NodeLeaseResponseProto =
    NodeManagementOuterClass.NodeLeaseResponseProto.newBuilder()
        .setStatus(NodeManagementOuterClass.NodeLeaseResponseProto.Status.OK)
        .setInfo(toNodeInfoProto())
        .build()


fun Throwable.toNodeLeaseRequestResponseProto(): NodeManagementOuterClass.NodeLeaseResponseProto =
    NodeManagementOuterClass.NodeLeaseResponseProto.newBuilder()
        .setStatus(NodeManagementOuterClass.NodeLeaseResponseProto.Status.ERROR)
        .setErrorDescription(toString())
        .build()