/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture

object DeferredWrappers {
    fun wrapReturn(deferred: Deferred<*>, method: Method): Any =
        when (method.returnType) {
            Deferred::class.java -> deferred
            CompletableFuture::class.java -> deferred.asCompletableFuture()
            else -> {
                throw IllegalArgumentException("No async wrapper for ${method.returnType} found")
            }
        }

    fun wrapCall(result: Any) : Deferred<*> =
        when(result) {
            is Deferred<*> -> result
            is CompletableFuture<*> -> result.asDeferred()
            else -> {
                throw IllegalArgumentException("No async wrapper for ${result.javaClass} found")
            }
        }
}