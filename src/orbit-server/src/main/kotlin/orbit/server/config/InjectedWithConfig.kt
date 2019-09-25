/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.config

import orbit.common.di.ComponentProviderRoot

interface InjectedWithConfig<T> {
    val instanceType: Class<out T>
}

fun <T : Any> ComponentProviderRoot.injectedWithConfig(config: InjectedWithConfig<T>) {
    componentProvider.registerDefinition(config.instanceType)
    componentProvider.registerInstance(config.javaClass, config)
}