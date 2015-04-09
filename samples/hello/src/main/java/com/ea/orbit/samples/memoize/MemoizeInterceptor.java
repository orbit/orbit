package com.ea.orbit.samples.memoize;

import com.ea.orbit.actors.IAddressable;
import com.ea.orbit.actors.runtime.IRuntime;
import com.ea.orbit.actors.runtime.InvokeInterceptor;
import com.ea.orbit.concurrent.Task;
import net.jodah.expiringmap.ExpiringMap;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MemoizeInterceptor implements InvokeInterceptor {

    ExpiringMap<String, Task> memoizeMap = ExpiringMap.builder().variableExpiration().build();

    @Override
    public Task invoke(IRuntime runtime, IAddressable toReference, Method m, boolean oneWay, int methodId, Object[] params) {
        if (m.isAnnotationPresent(Memoize.class))
        {
            Memoize ann = m.getAnnotation(Memoize.class);
            long memoizeMaxMillis = ann.unit().toMillis(ann.time());
            String key = Integer.toString(methodId) + "_" + Stream.of(params).map(p -> Integer.toString(p.hashCode())).collect(Collectors.joining("_"));
            Task cached = memoizeMap.get(key);
            if (cached == null)
            {
                cached = runtime.sendMessage(toReference, oneWay, methodId, params);
                memoizeMap.put(key, cached, ExpiringMap.ExpirationPolicy.CREATED, memoizeMaxMillis, TimeUnit.MILLISECONDS);
            }
            return cached;
        }
        return runtime.sendMessage(toReference, oneWay, methodId, params);
    }

}
