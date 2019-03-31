/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.core.remoting.AddressableClass
import cloud.orbit.runtime.remoting.AddressableDefinitionDirectory
import cloud.orbit.runtime.remoting.AddressableImplDefinition
import cloud.orbit.runtime.special.remoting.EmptyClassAddressable
import cloud.orbit.runtime.special.remoting.EmptyInterfaceAddressable
import cloud.orbit.runtime.special.remoting.FullInheritedClassAddressable
import cloud.orbit.runtime.special.remoting.InvalidMethodInterfaceAddressable
import cloud.orbit.runtime.special.remoting.LifecycleEventsClassAddressable
import cloud.orbit.runtime.special.remoting.RoutingExecutionClassAddressable
import cloud.orbit.runtime.special.remoting.RoutingInterfaceAddressable
import cloud.orbit.runtime.special.remoting.RoutingLifecycleClassAddressable
import cloud.orbit.runtime.special.remoting.ValidMethodInterfaceAddressable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test


class AddressableDictionaryTest {
    private val addressableInterfaceDefinitionDictionary = AddressableDefinitionDirectory()

    private fun generateImpl(implClass: AddressableClass): AddressableImplDefinition {
        addressableInterfaceDefinitionDictionary.setupDefinition(
            listOf(),
            mapOf(RoutingInterfaceAddressable::class.java to implClass)
        )
        return addressableInterfaceDefinitionDictionary.getImplDefinition(RoutingInterfaceAddressable::class.java)
    }

    @Test
    fun `check interface fails on classes`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreateInterfaceDefinition(EmptyClassAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("not an interface")
    }

    @Test
    fun `check missing routing fails`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreateInterfaceDefinition(EmptyInterfaceAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("routing")
    }

    @Test
    fun `check invalid return type fails`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreateInterfaceDefinition(InvalidMethodInterfaceAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("asynchronous")
    }

    @Test
    fun `check valid return type passes`() {
        addressableInterfaceDefinitionDictionary.getOrCreateInterfaceDefinition(ValidMethodInterfaceAddressable::class.java)
    }

    @Test
    fun `check routing only interface passes`() {
        addressableInterfaceDefinitionDictionary.getOrCreateInterfaceDefinition(RoutingInterfaceAddressable::class.java)
    }

    @Test
    fun `check missing execution model fails`() {
        assertThatThrownBy {
            generateImpl(RoutingLifecycleClassAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("execution")
    }

    @Test
    fun `check missing lifecycle model fails`() {
        assertThatThrownBy {
            generateImpl(RoutingExecutionClassAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("lifecycle")
    }

    @Test
    fun `check inherit all passes`() {
        generateImpl(FullInheritedClassAddressable::class.java)
    }

    @Test
    fun `check lifecycle events not found`() {
        val def = generateImpl(FullInheritedClassAddressable::class.java)
        assertThat(def.onDeactivateMethod).isNull()
        assertThat(def.onActivateMethod).isNull()
    }

    @Test
    fun `check lifecycle events found`() {
        val def = generateImpl(LifecycleEventsClassAddressable::class.java)
        assertThat(def.onDeactivateMethod).isNotNull
        assertThat(def.onActivateMethod).isNotNull
    }
}