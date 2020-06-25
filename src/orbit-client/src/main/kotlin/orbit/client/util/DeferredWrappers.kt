/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import java.lang.reflect.Method
import java.util.concurrent.CompletionStage
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.kotlinFunction

internal object DeferredWrappers {
    private val supportedWrappers = listOf(
        CompletionStage::class.java,
        Deferred::class.java
    )

    fun isAsync(method: Method): Boolean =
        supportedWrappers.any { it.isAssignableFrom(method.returnType) }
                || (method.kotlinFunction?.isSuspend == true)

    fun wrapReturn(deferred: Deferred<*>, method: Method, coroutineScope: CoroutineScope? = null): Any =
        when {
            CompletionStage::class.java.isAssignableFrom(method.returnType) -> deferred.asCompletableFuture()
            Deferred::class.java.isAssignableFrom(method.returnType) -> deferred
            else -> {
                throw IllegalArgumentException("No async wrapper for ${method.returnType} found")
            }
        }

    fun wrapCall(result: Any): Deferred<*> =
        when (result) {
            is CompletionStage<*> -> result.asDeferred()
            is Deferred<*> -> result
            else -> {
                throw IllegalArgumentException("No async wrapper for ${result.javaClass} found")
            }
        }

    suspend fun wrapSuspend(
        method: Method,
        instance: Any,
        argValues: Array<Any?> = emptyArray()
    )  = coroutineScope {
        CompletableDeferred<Any?>().let { deferred ->
            method.invoke(
                instance, *argValues.plus(
                    Continuation<Any?>(coroutineContext) { r -> deferred.complete(r) })
            )
        }
    }

}