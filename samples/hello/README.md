
Hello world example
===================

```java
public interface IHello extends IActor
{
    Task<String> sayHello(String greeting);
}

IHello helloActor = IActor.getReference(IHello.class, "0");
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
```
start-multiple.bat
```

Stop the nodes with `ctrl+d`

### Linux
Start a each node with:
```
mvn exec:java
```

Stop the nodes with `ctrl+z`

### Sample output

```
-------------------------------------------------------------------
GMS: address=helloWorldCluster, cluster=ISPN, physical address=192.168.0.10:52155
-------------------------------------------------------------------
You said: 'Hi from Orbit[1jue6LU5Q/uwXebNT8YQ7A]', I say: Hello from Orbit[lUrbzZw0Squmn5Ox3kTK3w] !

Type a message an press enter, or run other instances and see what happens.
-->hello orbit!
You said: 'hello orbit!', I say: Hello from Orbit[lUrbzZw0Squmn5Ox3kTK3w] !
-->_

```
