/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import java.lang.reflect.Method

internal class MethodSerializer: Serializer<Method>(false, true) {
    override fun write(kryo: Kryo, output: Output, method: Method) {
        kryo.writeClass(output, method.declaringClass)
        output.writeString(method.name)
        kryo.writeClassAndObject(output, method.parameterTypes)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Method>): Method {
        val clazz = kryo.readClass(input).type
        val methodName = input.readString()
        @Suppress("UNCHECKED_CAST")
        val params = kryo.readClassAndObject(input) as Array<Class<*>>
        return clazz.getDeclaredMethod(methodName, *params)
    }

    override fun copy(kryo: Kryo, original: Method): Method = original
}
