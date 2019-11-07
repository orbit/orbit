/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import kotlinx.coroutines.Deferred
import orbit.client.util.DeferredWrappers
import orbit.shared.addressable.AddressableInvocationArguments
import java.lang.reflect.Method

internal object MethodInvoker {
    private fun invokeRaw(instance: Any, method: Method, args: AddressableInvocationArguments): Any {
        method.isAccessible = true
        return method.invoke(instance, *args.map { it.first }.toTypedArray())
    }

    fun invokeDeferred(instance: Any, methodName: String, args: AddressableInvocationArguments): Deferred<*> =
        instance::class.java.getMethod(
            methodName,
            *(args.map { it.second }.toTypedArray())
        ).let { method ->
            method.isAccessible = true
            method.invoke(instance, *(args.map { it.first }.toTypedArray()))
        }.let {
            DeferredWrappers.wrapCall(it)
        }
}