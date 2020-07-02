/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import orbit.client.util.DeferredWrappers
import orbit.shared.addressable.AddressableInvocation
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

internal class AddressableProxy(
    private val reference: AddressableReference,
    private val invocationSystem: InvocationSystem
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any {
        val isSuspended = args?.lastOrNull() is Continuation<*>
        val continuation = (if (isSuspended) {
            args!!.last()
        } else null) as Continuation<Any?>?

        val mappedArgs = (if (isSuspended) {
            args!!.take(args.size - 1).toTypedArray()
        } else args)
            ?.mapIndexed { index, value ->
                value to method.parameterTypes[index]
            }?.toTypedArray() ?: emptyArray()

        val invocation = AddressableInvocation(
            reference = reference,
            method = method.name,
            args = mappedArgs
        )

        val completion = invocationSystem.sendInvocation(invocation)
        return if (isSuspended) {
            completion.invokeOnCompletion { error ->
                if (error == null)
                    continuation?.resume(completion.getCompleted())
                else
                    continuation?.resumeWith(Result.failure(error))
            }
            kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
        } else {
            DeferredWrappers.wrapReturn(completion, method)
        }
    }
}

internal class AddressableProxyFactory(
    private val invocationSystem: InvocationSystem
) {
    fun <T : Addressable> createProxy(interfaceClass: Class<T>, key: Key): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(interfaceClass),
            AddressableProxy(
                reference = AddressableReference(
                    type = interfaceClass.canonicalName,
                    key = key
                ),
                invocationSystem = invocationSystem
            )
        ) as T
    }
}
