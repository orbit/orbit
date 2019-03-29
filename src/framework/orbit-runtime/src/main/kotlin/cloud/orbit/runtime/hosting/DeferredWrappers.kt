/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.hosting

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture

internal object DeferredWrappers {
    fun wrapReturn(deferred: Deferred<*>, method: Method): Any =
        when (method.returnType) {
            CompletableFuture::class.java -> deferred.asCompletableFuture()
            CompletableDeferred::class.java -> deferred
            Deferred::class.java -> deferred
            else -> {
                throw IllegalArgumentException("No async wrapper for ${method.returnType} found")
            }
        }

    fun wrapCall(result: Any): Deferred<*> =
        when (result) {
            is CompletableFuture<*> -> result.asDeferred()
            is CompletableDeferred<*> -> result
            is Deferred<*> -> result
            else -> {
                throw IllegalArgumentException("No async wrapper for ${result.javaClass} found")
            }
        }
}