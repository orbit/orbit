# Dependency Injection

By default, Orbit will instantiate and activate Addressables using the default, empty constructor. Many times an Addressable will require some service or store for activation or its functional use.

Orbit supports a "bring your own DI container" model

The typical approach is to use constructor dependency injection, where dependencies are be supplied as constructor arguments.

```kotlin
class PlayerImpl(private val playerStore: PlayerStore) : AbstractActor(), Player {
    ... PlayerImpl code
}
```

To wire a DI container into Orbit, the `addressableConstructor` member of `OrbitConfig` can be replaced with a custom implementation of the `AddressableConstructor` interface. Below is an example 

```kotlin
class KodeinAddressableConstructor(private val kodein: Kodein) : AddressableConstructor {
    object KodeinAddressableConstructorSingleton : ExternallyConfigured<AddressableConstructor> {
        override val instanceType = KodeinAddressableConstructor::class.java
    }

    override fun constructAddressable(clazz: Class<out Addressable>): Addressable {
        val addressable: Addressable by kodein.Instance(TT(clazz))

        return addressable
    }
}
```

In this case, the `KodeinAddressableConstructor` requires a Kodein instance in its constructor. The instance can be supplied to the internal Orbit container through the `containerOverrides` member of OrbitConfig which will supply it automatically when the `KodeinAddressableConstructor` is created.

```kotlin
import orbit.carnival.actors.PlayerImpl
import orbit.carnival.actors.repository.PlayerStore
import orbit.carnival.actors.repository.etcd.EtcdPlayerStore
import org.kodein.di.*
import org.kodein.di.generic.*

fun main() {
    runBlocking {
        ...
        val kodein = Kodein {
            bind<PlayerStore>() with singleton { EtcdPlayerStore(storeUrl) }
            bind<PlayerImpl>() with provider { PlayerImpl(instance()) }
        }

        val orbitClient = OrbitClient(
            OrbitClientConfig(
                ...
                addressableConstructor = KodeinAddressableConstructor.KodeinAddressableConstructorSingleton,
                containerOverrides = {
                    instance(kodein)
                }
            )
        )
        ...
    }
}
```

