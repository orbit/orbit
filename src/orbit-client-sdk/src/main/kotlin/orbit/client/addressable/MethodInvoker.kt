/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import kotlinx.coroutines.Deferred
import orbit.client.util.DeferredWrappers
import java.lang.reflect.Method

internal object MethodInvoker {
    private fun invokeRaw(instance: Any, method: Method, args: Array<out Any?>): Any {
        method.isAccessible = true
        return method.invoke(instance, *args)
    }

    fun invokeDeferred(instance: Any, method: String, args: Array<out Any?>): Deferred<*> =
        invokeRaw(
            instance, instance::class.java.getMethod(
                method,
                *(args.map { if (it == null) Any::class.java else it::class.java }.toTypedArray())
            ),
            args
        ).let {
            DeferredWrappers.wrapCall(it)
        }

    fun invokeDeferred(instance: Any, method: Method, args: Array<out Any?>): Deferred<*> =
        invokeRaw(instance, method, args).let {
            DeferredWrappers.wrapCall(it)
        }
}