package cloud.orbit.actors.runtime;

import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.Reentrant;
import cloud.orbit.concurrent.Task;
import cloud.orbit.util.AnnotationCache;

import java.lang.reflect.Method;
import java.util.Map;

public abstract class InvocationHandler {

    private boolean myResult;
    private Task result;

    boolean is() {
        return myResult;
    }

    public Task getResult() {
        return result;
    }

    public abstract void beforeInvoke(Invocation invocation, Method method);

    public abstract void afterInvoke(long startTimeMs, Invocation invocation, Method method);

    public abstract void taskComplete(long startTimeMs, Invocation invocation, Method method);

    public InvocationHandler invoke(Stage runtime, AnnotationCache<Reentrant> reentrantCache, Invocation invocation, LocalObjects.LocalObjectEntry entry, LocalObjects.LocalObjectEntry target, ObjectInvoker invoker) {

        boolean reentrant = false;

        final Method method;
        final long start;

        final ActorTaskContext context = ActorTaskContext.current();
        if (context != null) {
            if (invocation.getHeaders() != null && invocation.getHeaders().size() > 0 && runtime.getStickyHeaders() != null) {
                for (Map.Entry e : invocation.getHeaders().entrySet()) {
                    if (runtime.getStickyHeaders().contains(e.getKey())) {
                        context.setProperty(String.valueOf(e.getKey()), e.getValue());
                    }
                }
            }

            method = invoker.getMethod(invocation.getMethodId());

            if (reentrantCache.isAnnotated(method)) {
                reentrant = true;
                context.setDefaultExecutor(r -> entry.run(o -> {
                    r.run();
                    return Task.done();
                }));
            }
            context.setRuntime(runtime);
        } else {
            method = null;
            runtime.bind();
        }

        if (method != null) {
            beforeInvoke(invocation, method);
        }

        start = System.nanoTime();
        result = invoker.safeInvoke(target.getObject(), invocation.getMethodId(), invocation.getParams());

        if (method != null) {
            afterInvoke(start, invocation, method);
        }

        if (invocation.getCompletion() != null) {
            InternalUtils.linkFutures(result, invocation.getCompletion());
        }

        if (method != null) {
            result.thenAccept(n -> taskComplete(start, invocation, method));
        }

        if (reentrant) {
            // let the execution serializer proceed if actor method blocks on a task
            myResult = true;
            return this;
        }

        myResult = false;
        return this;
    }
}
