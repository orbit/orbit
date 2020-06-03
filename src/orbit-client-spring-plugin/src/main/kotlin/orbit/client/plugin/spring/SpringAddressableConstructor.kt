/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.plugin.spring

import orbit.client.addressable.Addressable
import orbit.client.addressable.AddressableConstructor
import orbit.util.di.ExternallyConfigured
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext

class SpringAddressableConstructor(private val config: Config) : AddressableConstructor {
    data class Config(
        val beanFactory: AutowireCapableBeanFactory
    ) : ExternallyConfigured<AddressableConstructor> {
        constructor(appContext: ApplicationContext): this(appContext.autowireCapableBeanFactory)

        override val instanceType: Class<out AddressableConstructor> = SpringAddressableConstructor::class.java
    }

    override fun constructAddressable(clazz: Class<out Addressable>) =
        config.beanFactory.createBean(clazz)
}