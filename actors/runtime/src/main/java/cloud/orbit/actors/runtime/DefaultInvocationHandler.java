package cloud.orbit.actors.runtime;

import java.lang.reflect.Method;

public class DefaultInvocationHandler extends InvocationHandler {

    @Override
    public void beforeInvoke(Invocation invocation, Method method) {
    }

    @Override
    public void afterInvoke(long startTimeMs, Invocation invocation, Method method) {
    }

    @Override
    public void taskComplete(long startTimeMs, Invocation invocation, Method method) {
    }
}
