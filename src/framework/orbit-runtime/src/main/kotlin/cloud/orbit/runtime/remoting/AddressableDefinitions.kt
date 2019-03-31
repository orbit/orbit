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
    val methods: Map<Method, AddressableInterfaceMethodDefinition>
)

internal data class AddressableInterfaceMethodDefinition(
    val method: Method
)

internal data class AddressableImplDefinition(
    val implClass: AddressableClass,
    val interfaceClass: AddressableClass,
    val interfaceDefinition: AddressableInterfaceDefinition,
    val methods: Map<Method, AddressableImplMethodDefinition>,
    val lifecycle: Lifecycle,
    val executionModel: ExecutionModel,
    val onActivateMethod: AddressableImplMethodDefinition?,
    val onDeactivateMethod: AddressableImplMethodDefinition?

)

internal data class AddressableImplMethodDefinition(
    val method: Method,
    val isOnActivate: Boolean,
    val isOnDeactivate: Boolean
)