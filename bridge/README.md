Orbit Framework Bridge
=======

#####This is proof-of-concept of a bridge to use REST to simplify how external systems can access cluster actors and actors can access external systems.

How to use it
=======

#####The @Bridge annotation is used to mark the methods to be exposed.

```java
@StatelessWorker
public interface ISample1 extends IActor {
    @Bridge(path="/sample1/{id}/sum/{arg0}/{arg1}")
    Task<Integer> sum(int a,int b);
}

@NoIdentity
public interface ISample2 extends IActor {
    @Bridge(path="/sample2/getblogpost/{arg0}")
    Task<SampleBlogPost> getblogpost(int postNumber);
}
```

#####The framework also allows the actor to make external calls:
```java
IBridge bridgeClient = IActor.getReference(IBridge.class);
BridgeResult result = bridgeClient.call("http://jsonplaceholder.typicode.com/posts/" + postNumber, SampleBlogPost.class).join();
```
How to test it locally
=======

#####RUN "com.ea.orbit.samples.bridge.ServerMain"

#####type at the browser:
- http://localhost:8182/sample1/1235/sum/12/32
- http://localhost:8182/sample2/buy/something
- http://localhost:8182/sample2/getblogpost/4

Notes
=======

* The bridge uses the Restlet framework (http://restlet.com/products/restlet-framework/)
* The Restlet container has many connectors, that can deal with different levels of scalability. (http://restlet.com/technical-resources/restlet-framework/guide/2.3/core/base/connectors)
* This is a POC, there are a lot of TODOs, like making the IBridge call() method really asynchronous, and make proper tests.