/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.common.logging.logger
import cloud.orbit.runtime.di.ComponentProvider
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class KryoSerializer(componentProvider: ComponentProvider) {
    private val logger by logger()
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

    fun <T> deserializeObject(data: ByteArray, clazz: Class<T>): T {
        val kryo = kryoRef.get()
        val input = inputRef.get()
        input.reset()
        input.buffer = data

        val result = kryo.readClassAndObject(input)
        try {
            @Suppress("UNCHECKED_CAST")
            return result as T
        } catch (classCastException: ClassCastException) {
            val err =
                "Error deserializing. Was expecting ${clazz.simpleName} but received ${result.javaClass.simpleName}"
            logger.error(err)
            throw classCastException
        }
    }

    fun <T> cloneObject(obj: T): T {
        return kryoRef.get().copy(obj)
    }
}