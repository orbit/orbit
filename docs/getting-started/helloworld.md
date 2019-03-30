# Hello World

## Actor Interface

In Orbit all actors must have an interface, below we’ll create a very simple actor interface.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
interface Greeter : ActorWithNoKey {
    fun greet(name: String): Deferred<String>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
interface Greeter extends ActorWithNoKey {
    CompletableFuture<String> greet(String name);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

* Actor interfaces are standard interfaces with special constraints.
* All Actor interfaces must extend an Actor type.
* All interface methods must return a promise type.
* The return type \(if any\) of all methods must be serializable.

## Actor Implementation

Once you have an actor interface in place, the final step to complete the actor is to create an actor implementation.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
class GreeterActor : Greeter, AbstractActor() {
    private val logger by logger()

    override fun greet(name: String): Deferred<String> {
        logger.info("I was called by: $name.")
        return CompletableDeferred("Hello $name!")
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
public class GreeterActor extends AbstractActor implements Greeter {
    private static Logger logger = Logging.getLogger(GreeterActor.class);

    @Override
    public CompletableFuture<String> greet(String name) {
        logger.info("I was called by: " + name + ".");
        return CompletableFuture.completedFuture("Hello " + name + "!");
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

* An actor implementation is a standard class.
* All actors must extend AbstractActor.
* The actor must implement an actor interface.
* Only one actor implementation per actor interface is permitted.

## Using the Actor

The final step to get a working example is for us to actually use the actor.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
fun main() {
    val logger = getLogger("main")
    val stage = Stage()

    runBlocking {
        stage.start().await()
        val greeter = stage.actorProxyFactory.getReference<Greeter>()
        val greeting = greeter.greet("Joe").await()
        logger.info("Response: $greeting")
        stage.stop().await()
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
public static void main(String[] args) {
        Logger logger = Logging.getLogger("main");
        Stage stage = new Stage();
        stage.start().join();
        Greeter greeter = stage.getActorProxyFactory().getReference(Greeter.class);
        String greeting = greeter.greet("Joe").join();
        logger.info("Response: " + greeting);
        stage.stop();
    }
```
{% endcode-tabs-item %}
{% endcode-tabs %}

* We create an orbit execution environment known as a stage.
* We get a reference to the actor.
* The framework will handle the activation of the actor.
* You can communicate with the actor without knowing it's status.

## Running

If all went well you should now be able to run your application, the output should be similar to the following.

```text
[orbit-cpu-1] INFO cloud.orbit.runtime.stage.Stage - Starting Orbit...
[orbit-cpu-1] INFO cloud.orbit.runtime.stage.Stage - Orbit Environment: ClusterName(value=orbit-cluster) NodeIdentity(value=dHXsdERmqcCcXy3A) VersionInfo(orbitVersion=dev, orbitSpecVersion=dev, orbitCodename=Orbit, jvmVersion=1.8.0_202, jvmBuild=Azul Systems, Inc. OpenJDK 64-Bit Server VM 25.202-b05, kotlinVersion=1.3.21)
[orbit-cpu-1] INFO cloud.orbit.runtime.capabilities.CapabilitiesScanner - Scanning for node capabilities...
[orbit-cpu-1] INFO cloud.orbit.runtime.capabilities.CapabilitiesScanner - Node capabilities scan complete in 607ms. 1 implemented addressable(s) found. 1 addressable interface(s) found. 1 addressable class(es) found. 
[orbit-cpu-1] INFO cloud.orbit.runtime.pipeline.PipelineSystem - Pipeline started on 128 rails with a 10000 entries buffer and 5 steps.
[orbit-cpu-1] INFO cloud.orbit.runtime.stage.Stage - Orbit started successfully in 744ms.
[orbit-cpu-3] INFO orbit.helloworld.GreeterActor - I was called by: Joe. My identity is AddressableReference(interfaceClass=interface orbit.helloworld.Greeter, key=cloud.orbit.core.key.Key$NoKey@18e8d229)
[main] INFO main - Response: Hello Joe!
[orbit-cpu-9] INFO cloud.orbit.runtime.stage.Stage - Orbit stopping...
[orbit-cpu-1] INFO cloud.orbit.runtime.stage.Stage - Orbit stopped in 8ms.

```

