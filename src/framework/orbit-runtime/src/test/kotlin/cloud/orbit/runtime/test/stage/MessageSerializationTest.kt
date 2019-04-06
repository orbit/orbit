/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.test.stage

import cloud.orbit.common.util.RandomUtils
import cloud.orbit.core.annotation.ExecutionModel
import cloud.orbit.core.annotation.Lifecycle
import cloud.orbit.core.annotation.Routing
import cloud.orbit.core.hosting.ExecutionStrategy
import cloud.orbit.core.hosting.RandomRouting
import cloud.orbit.core.hosting.createProxy
import cloud.orbit.core.key.Key
import cloud.orbit.core.remoting.AbstractAddressable
import cloud.orbit.core.remoting.Addressable
import cloud.orbit.runtime.serialization.kryo.DEFAULT_KRYO_BUFFER_SIZE
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.util.*

@Routing(true, true, RandomRouting::class, true)
interface SerializationTestAddressable : Addressable {
    fun doNothing(): Deferred<Unit>
    fun echoString(value: String): Deferred<String>
    fun echoInt(value: Int): Deferred<Int>
    fun echoGuid(value: UUID): Deferred<UUID>
    fun echoList(value: List<String>): Deferred<List<String>>
    fun echoClass(value: Class<out Any>): Deferred<Class<out Any>>
    fun echoMethod(value: Method): Deferred<Method>
    fun overloaded(value: String): Deferred<String>
    fun overloaded(value: Int): Deferred<String>
    fun getKey(): Deferred<Key>
    fun selfRefGetId(value: SerializationTestAddressable): Deferred<Key>
    fun returnRef(value: Key): Deferred<SerializationTestAddressable>
    fun returnSelfRef(): Deferred<SerializationTestAddressable>
}

@ExecutionModel(ExecutionStrategy.SAFE)
@Lifecycle(true, true)
@Suppress("UNUSED")
class SerializationTestAddressableImpl : SerializationTestAddressable, AbstractAddressable() {
    override fun doNothing() = CompletableDeferred(Unit)
    override fun echoString(value: String) = CompletableDeferred(value)
    override fun echoInt(value: Int) = CompletableDeferred(value)
    override fun echoGuid(value: UUID) = CompletableDeferred(value)
    override fun echoList(value: List<String>) = CompletableDeferred(value)
    override fun echoClass(value: Class<out Any>) = CompletableDeferred(value)
    override fun echoMethod(value: Method) = CompletableDeferred(value)
    override fun overloaded(value: String) = CompletableDeferred("String=$value")
    override fun overloaded(value: Int) = CompletableDeferred("Int=$value")
    override fun getKey() = CompletableDeferred(context.reference.key)
    override fun selfRefGetId(value: SerializationTestAddressable) = value.getKey()
    override fun returnRef(value: Key): Deferred<SerializationTestAddressable> =
        CompletableDeferred(
            context.runtime.addressableRegistry.createProxy(SerializationTestAddressable::class.java, value)
        )

    override fun returnSelfRef() = CompletableDeferred(this)

}

abstract class MessageSerializationTest : BaseStageTest() {
    @Test
    fun `ensure all key types succeed`() {
        val echoMsg = "Wormhole"
        val noKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        val intKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.Int32Key(1234))
        val longKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.Int64Key(5432))
        val stringKey = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.StringKey("Milky Way"))
        val guidKey =
            stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.GuidKey(UUID.randomUUID()))

        runBlocking {
            noKey.echoString(echoMsg).await()
            intKey.echoString(echoMsg).await()
            longKey.echoString(echoMsg).await()
            stringKey.echoString(echoMsg).await()
            guidKey.echoString(echoMsg).await()
        }
    }

    @Test
    fun `ensure multiple basic arg types succeed`() {
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        runBlocking {
            val nothingVal = Unit
            val nothingRes = echo.doNothing().await()
            assertThat(nothingRes).isEqualTo(nothingVal)

            val strVal = "Pegasus Galaxy"
            val strRes = echo.echoString(strVal).await()
            assertThat(strRes).isEqualTo(strVal)

            val intVal = 123253
            val intRes = echo.echoInt(intVal).await()
            assertThat(intRes).isEqualTo(intVal)

            val uuidVal = UUID.randomUUID()
            val uuidRes = echo.echoGuid(uuidVal).await()
            assertThat(uuidRes).isEqualTo(uuidVal)

            val listVal = listOf("Jack O'Neill", "Samantha Carter", "Daniel Jackson", "Teal'c")
            val listRes = echo.echoList(listVal).await()
            assertThat(listRes).isEqualTo(listVal)

            val classVal = StageConfig::class.java
            val classRes = echo.echoClass(classVal).await()
            assertThat(classRes).isEqualTo(classVal)

            val methodVal = SerializationTestAddressable::class.java.getDeclaredMethod("doNothing")
            val methodRes = echo.echoMethod(methodVal).await()
            assertThat(methodVal.name).isEqualTo(methodRes.name)
        }
    }

    @Test
    fun `ensure copy really happens`() {
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        runBlocking {
            val listVal = listOf("Abydos", "Dakara", "Othala", "Orilla")
            val listRes = echo.echoList(listVal).await()
            assertThat(listRes).isNotSameAs(listVal)
            assertThat(listRes).isEqualTo(listVal)
        }
    }

    @Test
    fun `ensure can pass reference`() {
        val otherId = "Daedalus"
        val mainRef = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        val otherRef = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.StringKey(otherId))

        runBlocking {
            val result = mainRef.selfRefGetId(otherRef).await()
            assertThat(result).isNotSameAs(otherId)
        }
    }

    @Test
    fun `ensure can receive reference`() {
        val otherId = Key.StringKey("Prometheus")
        val mainRef = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)

        runBlocking {
            val resultRef = mainRef.returnRef(otherId).await()
            val resultKey = resultRef.getKey().await()
            assertThat(resultKey).isEqualTo(otherId)
        }
    }

    @Test
    fun `ensure real instance gets boxed`() {
        val thisKey = Key.StringKey("Death Glider")
        val mainRef = stage.addressableRegistry.createProxy<SerializationTestAddressable>(thisKey)

        runBlocking {
            val resultRef = mainRef.returnSelfRef().await()
            val resultKey = resultRef.getKey().await()
            assertThat(resultKey).isEqualTo(thisKey)
        }
    }

    @Test
    fun `ensure method overloading has expected result`() {
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        runBlocking {
            val strVal = "Unscheduled off-world activation!"
            val strRes = echo.overloaded(strVal).await()
            assertThat(strRes).isEqualTo("String=$strVal")

            val intVal = 1969
            val intRes = echo.overloaded(intVal).await()
            assertThat(intRes).isEqualTo("Int=$intVal")
        }
    }

    @Test
    fun `ensure too large message succeeds`() {
        val echoMsg = RandomUtils.pseudoRandomString(DEFAULT_KRYO_BUFFER_SIZE * 2)
        val echo = stage.addressableRegistry.createProxy<SerializationTestAddressable>(Key.NoKey)
        val result = runBlocking {
            echo.echoString(echoMsg).await()
        }
        Assertions.assertThat(result).isEqualTo(echoMsg)
    }
}

class RawMessageSerializationTest : MessageSerializationTest() {
    override fun setupStage(stageConfig: StageConfig): StageConfig {
        return stageConfig.copy(
            allowLoopback = false
        )
    }
}

class CloneMessageSerializationTest : MessageSerializationTest() {
    override fun setupStage(stageConfig: StageConfig): StageConfig {
        return stageConfig.copy(
            allowLoopback = true
        )
    }
}