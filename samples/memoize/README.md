
Memoize Sample
===================

This is a custom Memoize annotation (result cache).
In clusters, strategically placed memoized methods can save a *lot* of IO inside the cluster and increase the scalability. In a real world scenario there is always information that can be cached for a few seconds.
It is also a proof-of-concept for the invoke interceptors.

###### Usage

adding the provider:
```java
stage.addProvider(new MemoizeHookProvider());
```

using in the actor:
```java
public interface IExample extends IActor {
    @Memoize(time = 5, unit = TimeUnit.SECONDS)
    Task<Long> getSomething(String greeting);
}
```





