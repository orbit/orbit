/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.di

import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import org.junit.Test

class ContainerTests {
    @Test
    fun `Registered factory gets called on resolve`() {
        val container = ComponentContainer()

        val test = TestClass()
        container.configure {
            register { -> test }
        }

        container.resolve<TestClass>() shouldBeSameInstanceAs test
    }

    @Test
    fun `Registered factory is uniquely called each resolve`() {
        val container = ComponentContainer()

        container.configure {
            register { -> TestClass() }
        }

        container.resolve<TestClass>() shouldNotBeSameInstanceAs container.resolve<TestClass>()
    }

    @Test
    fun `Unregistered type is constructed through default constructor`() {
        val container = ComponentContainer()

        val test = container.resolve<TestClass>()

        test.message("test message")
    }

    @Test
    fun `Unregistered type with dependencies is constructed recursively`() {
        val container = ComponentContainer()

        val test = container.resolve<TestClassWithDependencies>()

        test.message("Test Message through dependency")
    }

    @Test
    fun `Unregistered type uses registered dependency`() {
        val container = ComponentContainer()

        val dependency = TestClass()
        dependency.setPrefix("registered:")
        container.configure {
            register { _ -> dependency }
        }

        container.resolve<TestClassWithDependencies>().message("message") shouldBe "registered:message"
    }

    @Test
    fun `Registering an instance returns that instance`() {
        val container = ComponentContainer()

        val test = TestClass()
        container.configure {
            instance(test)
        }

        container.resolve<TestClass>() shouldBeSameInstanceAs container.resolve<TestClass>()
    }

    @Test
    fun `Registering a singleton returns same instance`() {
        val container = ComponentContainer()

        container.configure {
            singleton<TestClass>()
        }

        container.resolve<TestClass>() shouldBeSameInstanceAs  container.resolve<TestClass>()
    }

    @Test
    fun `Externally configured class resolved`() {
        val container = ComponentContainer()

        container.configure {
            externallyConfigured(ExternallyConfiguredClass.ExternallyConfiguredClassConfig("registered:"))
        }

        val test = container.resolve<ExternallyConfiguredClass>()

        test.message("message") shouldBe "registered:message"
    }

    class TestClass {
        private var prefix = ""
        fun setPrefix(prefix: String) {
            this.prefix = prefix
        }

        fun message(msg: String): String {
            println(prefix + msg)
            return prefix + msg
        }
    }

    class TestClassWithDependencies(private val dependency: TestClass) {
        fun message(msg: String): String {
            return dependency.message(msg)
        }
    }

    class ExternallyConfiguredClass(config: ExternallyConfiguredClassConfig) {
        data class ExternallyConfiguredClassConfig(val prefix: String) : ExternallyConfigured<ExternallyConfiguredClass> {
            override val instanceType: Class<out ExternallyConfiguredClass> = ExternallyConfiguredClass::class.java
        }

        private val prefix = config.prefix

        fun message(msg: String): String {
            println(prefix + msg)
            return prefix + msg
        }
    }
}