/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import cloud.orbit.runtime.pipeline.PipelineManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.asCompletableFuture
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture

class RemoteInterfaceProxy(
    private val pipelineManager: PipelineManager,
    private val interfaceDefinition: RemoteInterfaceDefinition,
    private val key: Key
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        val completion = CompletableDeferred<Any?>()
        val remoteInvocation = RemoteInvocation(
            interfaceDefinition = interfaceDefinition,
            methodDefinition = interfaceDefinition.methodDefinitions[method]!!,
            key = key,
            args = args ?: arrayOf(),
            completion = completion
        )

        pipelineManager.writeInvocation(remoteInvocation)

        return wrap(completion, method)
    }

    private fun wrap(completableDeferred: CompletableDeferred<*>, method: Method): Any =
        when(method.returnType) {
            CompletableDeferred::class.java -> completableDeferred
            CompletableFuture::class.java -> completableDeferred.asCompletableFuture()
            else -> {
                throw IllegalArgumentException("No async wrapper for ${method.returnType.toString()} found")
            }
        }
}