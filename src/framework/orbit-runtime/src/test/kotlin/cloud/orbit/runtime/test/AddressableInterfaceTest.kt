/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test

import cloud.orbit.runtime.remoting.AddressableInterfaceDefinitionDictionary
import cloud.orbit.runtime.special.remoting.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test


class AddressableInterfaceTest {
    private val addressableInterfaceDefinitionDictionary = AddressableInterfaceDefinitionDictionary()

    @Test
    fun `check fails on classes`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreate(ClassAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("not an interface")
    }

    @Test
    fun `check fails on non-concrete`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreate(NonConcreteAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("non-concrete")
    }

    @Test
    fun `check fails on missing routing`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreate(MissingRoutingAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("routing")
    }

    @Test
    fun `check fails on missing lifecycle`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreate(MissingLifecycleAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("lifecycle")
    }

    @Test
    fun `check fails on missing execution model`() {
        assertThatThrownBy {
            addressableInterfaceDefinitionDictionary.getOrCreate(MissingExecutionModelAddressable::class.java)
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("execution model")
    }

    @Test
    fun `check passes basic valid`() {
        val addressableDef = addressableInterfaceDefinitionDictionary.getOrCreate(BasicValidAddressable::class.java)
        assertThat(addressableDef.interfaceClass).isEqualTo(BasicValidAddressable::class.java)
    }

    @Test
    fun `check passes inherited valid`() {
        val addressableDef = addressableInterfaceDefinitionDictionary.getOrCreate(InheritedValidAddressable::class.java)
        assertThat(addressableDef.interfaceClass).isEqualTo(InheritedValidAddressable::class.java)
    }

    @Test
    fun `check passes methods valid`() {
        val addressableDef = addressableInterfaceDefinitionDictionary.getOrCreate(ValidMethodsAddressable::class.java)
        assertThat(addressableDef.interfaceClass).isEqualTo(ValidMethodsAddressable::class.java)
        assertThat(addressableDef.methods.size).isEqualTo(1)
    }
}