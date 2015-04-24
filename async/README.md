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

