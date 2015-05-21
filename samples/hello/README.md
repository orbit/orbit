
Hello World Sample
===================

```java
public interface Hello extends Actor
{
    Task<String> sayHello(String greeting);
}

public class HelloActor extends AbstractActor implements Hello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Received: " + greeting);
        return Task.fromValue("You said: '" + greeting + "', I say: Hello from " + runtimeIdentity() + " !");
    }
}

Hello helloActor = Actor.getReference(Hello.class, "0");
helloActor.sayHello("Hello!").join();
```


Building
--------
```
mvn clean install
```

Running
-------

Run multiple concurrent nodes to see that only one has
the Hello Actor active.

Type a message in the command line and find which node
has the actor.


### Windows
Start multiple nodes with:
```
start-multiple.bat
```

Stop the nodes with `ctrl+d`

### Linux
Start each node with:
```
mvn exec:java
```

Stop the nodes with `ctrl+z`

### Sample Output

```
-------------------------------------------------------------------
GMS: address=helloWorldCluster, cluster=ISPN, physical address=192.168.0.10:52155
-------------------------------------------------------------------
You said: 'Hi from Orbit[1jue6LU5Q/uwXebNT8YQ7A]', I say: Hello from Orbit[lUrbzZw0Squmn5Ox3kTK3w] !

Type a message and press enter, or run other instances and see what happens.
-->hello orbit!
You said: 'hello orbit!', I say: Hello from Orbit[lUrbzZw0Squmn5Ox3kTK3w] !
-->_

```
