/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import orbit.client.util.DeferredWrappers
import orbit.shared.addressable.AddressableInvocationArguments
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.kotlinFunction

internal object MethodInvoker {
    suspend fun invoke(
        instance: Any,
        methodName: String,
        args: AddressableInvocationArguments
    ): Any? = coroutineScope {
        val argTypes = args.map { it.second }.toTypedArray()

        val method = matchMethod(instance::class.java, methodName, argTypes) ?: matchMethod(
            instance::class.java,
            methodName,
            argTypes.plus(Continuation::class.java)
        ) ?: throw NoSuchMethodException("${methodName}(${argTypes.joinToString(", ")})")

        val argValues = args.map { it.first }.toTypedArray()

        method.isAccessible = true

        if (method.kotlinFunction?.isSuspend == true) {
            CompletableDeferred<Any?>().let { deferred ->
                method.invoke(
                    instance, *argValues.plus(
                        Continuation<Any?>(coroutineContext) { r -> deferred.complete(r) })
                )
            }
        } else {
            DeferredWrappers.wrapCall(method.invoke(instance, *argValues)).await()
        }
    }

    fun matchMethod(clazz: Class<*>, name: String, args: Array<Class<*>>): Method? =
        clazz.methods.firstOrNull { m -> m.name == name && m.parameterTypes.contentEquals(args) }
}
