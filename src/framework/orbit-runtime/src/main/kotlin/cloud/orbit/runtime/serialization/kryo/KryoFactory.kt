/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.key.Key
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.util.MapReferenceResolver
import org.objenesis.strategy.StdInstantiatorStrategy
import java.lang.reflect.Method

internal class KryoFactory {
    fun create(): Kryo {
        val kryo = Kryo(DefaultClassResolver(), MapReferenceResolver())

        kryo.isRegistrationRequired = false
        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

        // Orbit types
        kryo.addDefaultSerializer(Key.NoKey::class.java, KotlinObjectSerializer(Key.NoKey))

        // JDK Types
        kryo.addDefaultSerializer(Method::class.java, MethodSerializer())

        // Kotlin Types
        kryo.addDefaultSerializer(Unit::class.java, KotlinObjectSerializer(Unit))

        return kryo
    }
}