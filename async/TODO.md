
Maven plugin
---------------

 * Print instrumentation errors and warnings in the maven plugin
 * Turn misuse of await should be and error. With an option/annotation to disable the verification.
 * Ensure await.init is explicitly removed. With an option/annotation to disable the removal.

IDE integration
---------------

 * Intellij IDEA plugin: compile time instrumentation
 * Eclipse plugin: compile time instrumentation


Instrumentation
---------------

 * Treat `synchronized` blocks as errors? Or try to handle releasing/acquiring the lock.


Tests
------
 * test having a method type in the stack

Unlikely features
-----------------

 * Instrument methods that don't return task but are called with a wrapper?
   Probably not a good idea.

```java

void someMethod() {
    int a = 1;
    Task<Integer> res = async(()-> a + await(bla1()) + await(bla2()));
    // Equivalent to:
    // Task<Integer> res = async(()-> Task.fromValue(a + await(bla1()) + await(bla2())));
    doSomething(res);
}

void someMethod() {
    int a = 1;
    Task<Integer> res = async(()-> {
        int x = await(bla1());
        int y = await(bla2());
        return a + x + y;  // no task
    });
}

```



