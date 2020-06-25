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
        val method = matchMethod(instance::class.java, methodName, argTypes)
            ?: matchMethod(
                instance::class.java,
                methodName,
                argTypes.plus(Continuation::class.java)
            )

        if (method == null) {
            throw NoSuchMethodException("${methodName}(${argTypes.joinToString(", ")})")
        }

        val isSuspend = method.kotlinFunction?.isSuspend == true

        method.isAccessible = true
        if (isSuspend) {
            println("invoke suspended method")
            CompletableDeferred<Any?>().let { deferred ->
                method.invoke(
                    instance, *(args.map { it.first }.plus(
                        Continuation<Any?>(coroutineContext) { r -> deferred.complete(r) })).toTypedArray()
                )
            }
        } else {
            DeferredWrappers.wrapCall(method.invoke(instance, *(args.map { it.first }.toTypedArray()))).await()
        }
    }

    fun matchMethod(clazz: Class<*>, name: String, args: Array<Class<*>>): Method? =
        clazz.methods.firstOrNull { m -> m.name == name && m.parameterTypes.contentEquals(args) }

}
