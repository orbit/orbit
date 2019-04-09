/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference
import java.lang.reflect.Method

internal fun <T : Addressable> Class<T>.getOrCreateInterfaceDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableInterfaceDefinition = definitionDirectory.getOrCreateInterfaceDefinition(this)

internal fun AddressableReference.getOrCreateInterfaceDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableInterfaceDefinition = this.interfaceClass.getOrCreateInterfaceDefinition(definitionDirectory)

internal fun AddressableInvocation.getOrCreateInterfaceDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableInterfaceDefinition = this.reference.getOrCreateInterfaceDefinition(definitionDirectory)

internal fun <T : Addressable> Class<T>.getImplDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableImplDefinition = definitionDirectory.getImplDefinition(this)

internal fun AddressableReference.getImplDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableImplDefinition = this.interfaceClass.getImplDefinition(definitionDirectory)

internal fun AddressableInvocation.getImplDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableImplDefinition = this.reference.getImplDefinition(definitionDirectory)

internal fun AddressableInvocation.getInterfaceMethodDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableInterfaceMethodDefinition =
    getOrCreateInterfaceDefinition(definitionDirectory).let {
        it.methods.getValue(method)
    }

internal fun AddressableInvocation.getImplMethodDefinition(definitionDirectory: AddressableDefinitionDirectory)
        : AddressableImplMethodDefinition =
    getImplDefinition(definitionDirectory).let {
        it.methods.getValue(method)
    }

internal fun AddressableInterfaceDefinition.getMethod(method: Method): AddressableInterfaceMethodDefinition =
    this.methods.getValue(method)

internal fun AddressableImplDefinition.getMethod(method: Method): AddressableImplMethodDefinition =
    this.methods.getValue(method)