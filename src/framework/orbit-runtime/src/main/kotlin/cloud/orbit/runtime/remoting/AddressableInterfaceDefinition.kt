/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.remoting.AddressableClass
import java.lang.reflect.Method

internal data class AddressableInterfaceDefinition(
    val interfaceClass: AddressableClass,
    val routing: Routing,
    val lifecycle: Lifecycle,
    val executionModel: ExecutionModel,
    val methods: Map<Method, AddressableMethodDefinition>
)

internal data class AddressableMethodDefinition(
    val method: Method
)