/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import kotlinx.coroutines.CompletableDeferred

class RemoteInvocation(
    val interfaceDefinition: RemoteInterfaceDefinition,
    val methodDefinition: RemoteMethodDefinition,
    val key: Key,
    val args: Array<out Any?>,
    val completion: CompletableDeferred<Any?>
)