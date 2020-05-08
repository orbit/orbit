# Actor Interface Annotations
## StatelessWorker
```java
@StatelessWorker
interface MyActor extends Actor {}
```
Causes your actor to be a stateless worker. See [[stateless workers|Concepts: Stateless Workers]].

## NoIdentity
```java
@NoIdentity
interface MyActor extends Actor {}
```
Denotes that this actor does not have an identity and acts as a singleton. Actor is accessed using Actor.getReference(clazz) instead of Actor.getReference(clazz, id).

## PreferLocalPlacement
```java
@PreferLocalPlacement(percentile=100)
interface MyActor extends Actor {}
```
Denotes that this actor should prefer to be placed locally if not already activated and the local node is capable of hosting it. Optional percentile value allows developers to define the likelihood of preferring local placement (default 100). 

## NeverDeactivate
```java
@NeverDeactivate
interface MyActor extends Actor {}
```

Denotes that this actor should prefer not to deactivate once it has been activated, overriding the default deactivation due to inactivity. **Please Note**: This is a hint, the actor will not start automatically and may still be deactivated in the case of node failure. Applications should ping the actor regularly if they need to ensure it is always alive.

## TimeToLive
```java
@TimeToLive(value=10, timeUnit=TimeUnit.MINUTES)
interface MyActor extends Actor {}
```
Overrides the default time to live on an actor type.

# Message Interface Annotations
## OneWay
```java
@OneWay
Task someMessage();
```
This message is OneWay.  No result (value or status) will be returned, no guarantee that message will be processed.
The Task might contain an exception if there was a problem locating the target object or serializing the message.

## OnlyIfActivated
```java
@OnlyIfActivated
Task someMessage();
```
This message is only executed if the actor has already been activated.  Unlike normal messages, this will not cause an actor to activate.


## CacheResponse
```java
@CacheResponse(maxEntries = 1000, ttlDuration = 5, ttlUnit = TimeUnit.SECONDS)
Task<String> getAccountName(int id);
```
This message caches its result, on a per actor, per parameter-set basis.  The data persists for the given duration, or until at least the specified number of entries has been reached (whichever constraint is encountered first).  Cached values are not guaranteed to immediately evict once maxEntries has been reached.

Caches can be force-flushed using ExecutionCacheFlushManager.

## Timeout
```java
@Timeout(value=10, timeUnit=TimeUnit.SECONDS)
Task <String> getAccountName(); 
```
Overrides the default message timeout for the given actor message.

# Message Implementation Annotations
## Reentrant
```java
@Reentrant
Task<Void> doSomething() {}
```
Causes your message to be reentrant. See [[reentrancy|Concepts: Reentrancy]].

## SkipUpdateLastAccess
```java
@SkipUpdateLastAccess
Task<Void> doSomething() {}
```
Does not update the actors last access time thus preventing the message from extending the actor time to live.

# DTO Annotations
## Immutable
```java
@Immutable
class SomeMessageDTO {}
```
Denotes that an object is considered immutable and therefore the framework does not need to clone it. The application must ensure that the object is actually immutable and does not change once it has been passed to the framework. 