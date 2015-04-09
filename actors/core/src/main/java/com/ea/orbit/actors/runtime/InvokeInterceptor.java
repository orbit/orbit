package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.IAddressable;
import com.ea.orbit.concurrent.Task;

import java.lang.reflect.Method;

public interface InvokeInterceptor {
        Task invoke(IRuntime runtime, IAddressable toReference, Method method, boolean oneWay, int methodId, Object[] params);
}