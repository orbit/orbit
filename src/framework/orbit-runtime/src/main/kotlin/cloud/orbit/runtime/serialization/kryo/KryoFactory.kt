/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.key.Key
import cloud.orbit.runtime.di.ComponentProvider
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.util.MapReferenceResolver
import org.objenesis.strategy.StdInstantiatorStrategy

internal class KryoFactory(private val componentProvider: ComponentProvider) {
    fun create(): Kryo {
        val kryo = Kryo(DefaultClassResolver(), MapReferenceResolver())

        kryo.isRegistrationRequired = false
        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

        // Orbit types
        kryo.addDefaultSerializer(Key.NoKey::class.java, componentProvider.construct<NoKeySerializer>())

        return kryo
    }
}