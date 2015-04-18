Orbit Async
============

Orbit Async implements async-await methods in java. It allows programmers to write asyncronous code in a sequential fashion.

If you're looking for async await on the .NET CLR, see [Asynchronous Programming with Async and Await](https://msdn.microsoft.com/en-us/library/hh191443.aspx).

License
=======
Orbit is licensed under the [BSD 3-Clause License](../LICENSE).

Simple Examples
=======
#### With orbit Tasks
```java
@Async
public Task<Integer> getPageLength(URL url)
{
    Task<String> pageTask = getPage(url);

    // this will never block,
    // if pageTask is not completed getPageLength will return immediatelly.
    String page = await(pageTask);

    return Task.fromValue(page.length());
}

Task<Ingeger> lenTask = getPageLength(new URL("http://example.com"));
System.out.println(lenTask.join());
    
```
#### With CompletableFuture
```java
 import com.ea.orbit.async.Async;
 import com.ea.orbit.async.Await;
 import static com.ea.orbit.async.Await.await;
 
 public class Page
 {
    // has to be done at least once, usally in the main class.
    static { Await.init(); }

    @Async
    public CompletableFuture<Integer> getPageLength(URL url)
    {
        CompletableFuture<String> pageTask = getPage(url);

        // this will never block,
        // if pageTask is not completed getPageLength will return immediatelly.
        String page = await(pageTask);
 
        return CompletableFuture.completedFuture(page.length());
    }
    
 }

CompletableFuture<Ingeger> lenTask = getPageLength(new URL("http://example.com"));

System.out.println(lenTask.join());

```

