Orbit Async
============

Orbit Async implements async-await methods in the JVM. It allows programmers to write asynchronous code in a sequential fashion. It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com).

If you're looking for async await on the .NET CLR, see [Asynchronous Programming with Async and Await](https://msdn.microsoft.com/en-us/library/hh191443.aspx).

Documentation
=======
Documentation is located [here](http://orbit.bioware.com/).

License
=======
Orbit is licensed under the [BSD 3-Clause License](../LICENSE).

Simple Examples
=======
#### With Orbit Tasks
```java
import com.ea.orbit.async.Await;
import static com.ea.orbit.async.Await.await;
 
public class Page
{
    public Task<Integer> getPageLength(URL url)
    {
        Task<String> pageTask = getPage(url);
 
        // this will never block, it will return a promise
        String page = await(pageTask);
 
        return Task.fromValue(page.length());
    }
}

// do this once in the main class, or use the orbit-async-maven-plugin, or use -javaagent:orbit-async.jar
Await.init();

Task<Integer> lenTask = getPageLength(new URL("http://example.com"));
System.out.println(lenTask.join());

```
#### With CompletableFuture
```java
import com.ea.orbit.async.Async;
import com.ea.orbit.async.Await;
import static com.ea.orbit.async.Await.await;

public class Page
{
    // must mark CompletableFuture methods with @Async
    @Async
    public CompletableFuture<Integer> getPageLength(URL url)
    {
        CompletableFuture<String> pageTask = getPage(url);
        String page = await(pageTask);
        return CompletableFuture.completedFuture(page.length());
    }
 }

// do this once in the main class, or use the orbit-async-maven-plugin, or use -javaagent:orbit-async.jar
Await.init();

CompletableFuture<Integer> lenTask = getPageLength(new URL("http://example.com"));
System.out.println(lenTask.join());

```

Getting started
---------------

Orbit Async requires jvm 1.8.x as it relies on CompletableFuture, a new class.

It can work with java and scala and with any jvm language that generates jvm classes using methods with CompletableFuture, CompletionStage, or com.ea.orbit.concurrrent.Task return types.

### Using with maven

```xml
<dependency>
    <groupId>com.ea.orbit</groupId>
    <artifactId>orbit-async</artifactId>
    <version>${orbit.version}</version>
</dependency>
```

### Instumenting your code

#### Option 1 - Runtime
On your main class or as early as possible, call at least once:
```
Await.init();
```
Provided that your jvm has the capability enabled, this will start a runtime instrumentation agent.

This is the prefered solution for testing and development, it has the least amount of configuration.
If you forget to invoke this function the first call to `await` will initialize the system (and print a warning).

#### Option 2 - Jvm parameter

Start your application with an extra JVM parameter: `-javaagent:orbit-async-VERSION.jar`
```
 java -javaagent:orbit-async-VERSION.jar -cp your_claspath YourMainClass args...
```

#### Option 3 - Compile time instrumentation, with Maven

Use the [orbit-async-maven-plugin](maven-plugin).

This is the best option for libraries.

```
<build>
    <plugins>
        <plugin>
            <groupId>com.ea.orbit</groupId>
            <artifactId>orbit-async-maven-plugin</artifactId>
            <version>${orbit.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>instrument</goal>
                        <goal>instrument-test</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

