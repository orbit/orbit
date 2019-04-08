/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.hosting.AddressableRegistry
import cloud.orbit.core.remoting.RemoteAddressableReference
import cloud.orbit.runtime.hosting.ReferenceResolver
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class AddressableReferenceSerializer(
    private val referenceResolver: ReferenceResolver,
    private val addressableRegistry: AddressableRegistry
) : Serializer<Any?>(true, false) {

    override fun write(kryo: Kryo, output: Output, addressable: Any?) {
        if (addressable != null) {
            addressable.let {
                referenceResolver.resolveAddressableReference(addressable)
            }?.also {
                output.writeBoolean(true)

                kryo.writeObject(output, it)
            } ?: IllegalArgumentException("Addressable could not be resolved. $addressable")
        } else {
            output.writeBoolean(false)
        }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Any?>): Any? =
        when (input.readBoolean()) {
            true -> kryo.readObject(input, RemoteAddressableReference::class.java)
                .let {
                    addressableRegistry.createProxy(
                        it.reference.interfaceClass,
                        it.reference.key,
                        it.target
                    )
                }
            false -> null
        }

    override fun copy(kryo: Kryo, original: Any?): Any? =
        original?.let {
            referenceResolver.resolveAddressableReference(original)
        }?.let {
            addressableRegistry.createProxy(
                it.reference.interfaceClass,
                it.reference.key,
                it.target
            )
        } ?: IllegalArgumentException("Addressable could not be resolved. $original")
}