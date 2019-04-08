/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.remoting.Addressable
import cloud.orbit.core.remoting.AddressableInvocation
import cloud.orbit.core.remoting.AddressableReference

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
