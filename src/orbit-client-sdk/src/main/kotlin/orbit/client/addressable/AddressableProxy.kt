/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.addressable

import orbit.client.net.MessageHandler
import orbit.client.util.DeferredWrappers
import orbit.shared.addressable.Addressable
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class AddressableProxy(
    private val reference: AddressableReference,
    private val messageHandler: MessageHandler
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        val msg = Message(
            MessageContent.InvocationRequest(
                "Hello",
                reference
            )
        )
        val completion = messageHandler.sendMessage(msg)
        return DeferredWrappers.wrapReturn(completion, method)
    }
}

internal class AddressableProxyFactory(
    private val messageHandler: MessageHandler
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
                messageHandler = messageHandler
            )
        ) as T
    }

}