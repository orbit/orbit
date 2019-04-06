/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.remoting.AddressableRegistry
import cloud.orbit.runtime.hosting.ReferenceResolver
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class AddressableReferenceSerializer(
    private val referenceResolver: ReferenceResolver,
    private val addressableRegistry: AddressableRegistry
) :
    Serializer<Any?>(true, false) {

    override fun write(kryo: Kryo, output: Output, addressable: Any?) {
        if (addressable != null) {
            val reference = referenceResolver.resolveAddressableReference(addressable)
            if (reference != null) {
                output.writeBoolean(true)
                kryo.writeObject(output, reference)
                return
            } else {
                throw IllegalArgumentException("Addressable could not be resolved. $addressable")
            }
        }

        output.writeBoolean(false)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Any?>): Any? {
        val isNotNull = input.readBoolean()
        return if (isNotNull) {
            val reference = kryo.readObject(input, ReferenceResolver.RemoteAddressableReference::class.java)
            addressableRegistry.createProxy(
                reference.reference.interfaceClass,
                reference.reference.key,
                reference.target
            )
        } else {
            null
        }
    }

    override fun copy(kryo: Kryo, original: Any?): Any? {
        if (original != null) {
            val reference = referenceResolver.resolveAddressableReference(original)
            if (reference != null) {
                return addressableRegistry.createProxy(
                    reference.reference.interfaceClass,
                    reference.reference.key,
                    reference.target
                )
            }
            throw IllegalArgumentException("Addressable could not be resolved. $original")
        }
        return null
    }
}