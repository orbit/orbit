# Overview
Orbit uses slf4j internally to implement logging. Additionally orbit exposes logging custom logging to actors. 
**Please Note**: Orbit does not include an slf4j implementation by default so logging will be inactive unless a logging provider is supplied.

# Using Logging
### Logging In Actors
```kotlin
getLogger().info("Hello");
```

### Enabling Logging
By default Orbit does not provide an slf4j implementation.
Any slf4j provider will work so developers are able to choose whichever solution they like.

The most basic logging provider to include is the sl4j-simple provider implementation.

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.21</version>
</dependency>
```