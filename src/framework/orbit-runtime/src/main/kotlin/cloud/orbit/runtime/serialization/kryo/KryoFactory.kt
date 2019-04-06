/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.runtime.di.ComponentProvider
import com.esotericsoftware.kryo.ClassResolver
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.ReferenceResolver
import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.util.MapReferenceResolver
import org.objenesis.strategy.StdInstantiatorStrategy
import java.lang.reflect.Method

internal class KryoFactory(private val componentProvider: ComponentProvider) {
    private class WrappedKryo(classResolver: ClassResolver, referenceResolver: ReferenceResolver) :
        Kryo(classResolver, referenceResolver) {

        override fun getRegistration(type: Class<*>?): Registration {
            if (type != null && !type.isInterface) {
                if (Addressable::class.java.isAssignableFrom(type)) {
                    return getOrRegister(Addressable::class.java)
                }
            }

            return super.getRegistration(type)
        }

        private fun getOrRegister(clazz: Class<*>) =
            classResolver.getRegistration(clazz) ?: classResolver.registerImplicit(clazz)
    }

    fun create(): Kryo {
        val kryo = WrappedKryo(DefaultClassResolver(), MapReferenceResolver())

        kryo.isRegistrationRequired = false
        kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

        // Orbit types
        kryo.addDefaultSerializer(
            Addressable::class.java,
            componentProvider.construct<AddressableReferenceSerializer>()
        )
        kryo.addDefaultSerializer(Key.NoKey::class.java, KotlinObjectSerializer(Key.NoKey))

        // Basic Types
        kryo.addDefaultSerializer(Method::class.java, MethodSerializer())
        kryo.addDefaultSerializer(Unit::class.java, KotlinObjectSerializer(Unit))

        return kryo
    }
}