/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization

import cloud.orbit.runtime.di.ComponentProvider
import cloud.orbit.runtime.serialization.kryo.KryoSerializer

internal class SerializationSystem(componentProvider: ComponentProvider) {
    private val kryoSerializer: KryoSerializer = componentProvider.construct()

    fun <T> serializeObject(obj: T): ByteArray = kryoSerializer.serializeObject(obj)
    fun <T> deserializeObject(data: ByteArray, clazz: Class<T>): T = kryoSerializer.deserializeObject(data, clazz)
    inline fun <reified T> deserializeObject(data: ByteArray) = deserializeObject(data, T::class.java)
    fun <T> cloneObject(obj: T): T = kryoSerializer.cloneObject(obj)
}