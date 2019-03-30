/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import kotlinx.coroutines.Deferred
import java.lang.reflect.Method

internal object MethodInvoker {
    private fun invokeRaw(instance: Any, method: Method, args: Array<out Any?>): Any {
        method.isAccessible = true
        return method.invoke(instance, *args)
    }

    fun invokeDeferred(instance: Any, method: Method, args: Array<out Any?>): Deferred<*> {
        val rawResult = invokeRaw(instance, method, args)
        return DeferredWrappers.wrapCall(rawResult)
    }
}