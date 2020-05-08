Overview 
----------
Orbit follows the coding standards laid out in this document.

We realize that standards are often contentious and believe that having any standard (even where not everyone agrees) is the best course of action to ensure consistent and readable code across the project.

Naming Conventions 
----------

### Classes / Interfaces
**Use PascalCase** 
```java
public interface User extends Actor
{
}

public class UserActor extends AbstractActor implements User
{
}
```

### Enums

**Use PascalCase for name, SHOUTING_CASE for constants** 
```java
public enum StageMode
{
    FRONT_END, 
    HOST
}
```

### Methods 
**Use camelCase**
```java
public void doSomeStuff()
{
}
```
### Member Variables
**Use camelCase**
```java
public class SecurityFilter
{
    private int userId;
}
```

### Local Variables and Arguments
**Use camelCase**
```java
public static int sum(int leftHandSide, int rightHandSide)
{
    int totalSum = leftHandSide + rightHandSide;
    return totalSum;
}
```

### Constants
**Use SHOUTING_CASE**
```java
public static final byte NORMAL_MESSAGE = 0;
```

### Package Names
**Use lowercase** 
```java
package com.ea.orbit.actors;
```

Code Style
----------

### Braces
Opening braces should always be on a new line, always align the opening and closing of a block.
```java
if(someValue.equals("Ferrets"))
{
    if(someOtherValue.equals("Penguins"))
    {
    }
}
```

### Assignment / Comparison
Always include a space before and after an assignment or comparison.
```java
someValue += 1;
if(someValue == 1)
{
}
```

### Brackets
Never include a space after an opening bracket or before a closing bracket, optionally include one before an opening bracket.
```java
if (someValue.equals("Ferrets"))
if(someValue.equals("Ferrets"))
```

### Method/Class Annotations
Always annotate on a separate line above.
```java
@GET 
@PermitAll 
@Path("/healthCheck")
public HealthCheckDto getHealthCheck()
{
}
```

### Member Variable/Method Argument Annotations
Always annotate on a separate line above for member variables. Always annotate on the same line for method arguments.
```java
@Inject
StorageManager storageManager;
 
@Inject 
OrbitPropertiesProxy propertiesProxy;
 
public String getSomeValue(@Context SessionProxy sessionProxy)
{
}
```