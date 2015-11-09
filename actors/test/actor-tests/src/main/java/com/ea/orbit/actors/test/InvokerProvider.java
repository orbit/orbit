package com.ea.orbit.actors.test;

import com.ea.orbit.actors.runtime.ActorInvoker;

public interface InvokerProvider
{
    <T> ActorInvoker<T> getInvoker(int classId);

    int getInterfaceId(Class<?> clazz);
}
