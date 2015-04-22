Orbit Async REST Client
============

Allows calling JAX-RS REST interfaces with methods returning `CompletableFuture` or Orbit `Task`.
This enables writing REST client code in a fluent asynchronous way.    


Examples
========
Using Orbit tasks
-----

```java
public interface Hello
{
    @GET
    @Path("/")
    Task<String> getHome();
}

public static void main(String args[])
{
    WebTarget webTarget = getWebTarget("http://example.com");

    Hello hello = new OrbitRestClient(webTarget).get(Hello.class);

    // task is a CompletableFuture 
    Task<String> response = hello.getHome();

    response.thenAccept(x -> System.out.println(x)); 
    response.join();
}

// use any jax-rs client library to get javax.ws.rs.client.WebTarget
private static WebTarget getWebTarget(String host)
{
    ClientConfig clientConfig = new ClientConfig();
    Client client = ClientBuilder.newClient(clientConfig);
    return client.target(host);
}
```

Using CompletableFuture
-----

```java
public interface Hello
{
    @GET
    @Path("/")
    CompletableFuture<String> getHome();
}

public static void main(String args[])
{
    WebTarget webTarget = getWebTarget("http://example.com");

    Hello hello = new OrbitRestClient(webTarget).get(Hello.class);

    CompletableFuture<String> response = hello.getHome();
    
    response.thenAccept(x -> System.out.println(x)); 
    response.join();
}

// use any jax-rs client library to get javax.ws.rs.client.WebTarget
private static WebTarget getWebTarget(String host)
{
    ClientConfig clientConfig = new ClientConfig();
    Client client = ClientBuilder.newClient(clientConfig);
    return client.target(host);
}
```
