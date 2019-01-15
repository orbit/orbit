/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.key.Key
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class RemoteProxy(
    private val remoteInterfaceDefinition: RemoteInterfaceDefinition,
    private val key: Key
) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
        TODO("BEEP")
    }
}