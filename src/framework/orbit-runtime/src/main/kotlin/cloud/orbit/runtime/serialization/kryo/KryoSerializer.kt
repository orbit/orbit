/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.runtime.di.ComponentProvider
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class KryoSerializer(componentProvider: ComponentProvider) {
    private val kryoFactory: KryoFactory = componentProvider.construct()

    private val kryoRef = object : ThreadLocal<Kryo>() {
        override fun initialValue(): Kryo {
            return kryoFactory.create()
        }
    }

    private val inputRef = object : ThreadLocal<Input>() {
        override fun initialValue(): Input {
            return Input()
        }
    }

    private val outputRef = object : ThreadLocal<Output>() {
        override fun initialValue(): Output {
            return Output(DEFAULT_KRYO_BUFFER_SIZE, Int.MAX_VALUE)
        }
    }

    fun <T> serializeObject(obj: T): ByteArray {
        val kryo = kryoRef.get()
        val output = outputRef.get()
        output.reset()
        kryo.writeClassAndObject(output, obj)
        return output.toBytes()
    }

    fun <T> deserializeObject(data: ByteArray): T {
        val kryo = kryoRef.get()
        val input = inputRef.get()
        input.reset()
        input.buffer = data
        @Suppress("UNCHECKED_CAST")
        return kryo.readClassAndObject(input) as T
    }

    fun <T> cloneObject(obj: T) = kryoRef.get().copy(obj)
}