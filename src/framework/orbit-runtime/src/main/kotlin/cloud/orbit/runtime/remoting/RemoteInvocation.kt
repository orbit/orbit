/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key

data class RemoteInvocationTarget(
    val interfaceDefinition: RemoteInterfaceDefinition,
    val key: Key
)

class RemoteInvocation(
    val target: RemoteInvocationTarget,
    val methodDefinition: RemoteMethodDefinition,
    val args: Array<out Any?>
)