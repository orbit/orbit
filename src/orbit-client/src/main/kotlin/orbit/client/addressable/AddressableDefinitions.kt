/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import java.lang.reflect.Method

internal data class AddressableInterfaceDefinition(
    val interfaceClass: AddressableClass,
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
    val onActivateMethod: AddressableImplMethodDefinition?,
    val onDeactivateMethod: AddressableImplMethodDefinition?
)

internal data class AddressableImplMethodDefinition(
    val method: Method,
    val isOnActivate: Boolean,
    val isOnDeactivate: Boolean
)