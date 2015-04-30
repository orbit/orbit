package com.ea.orbit.actors.providers;

public interface IInvokeListenerProvider extends IOrbitProvider
{

    default void preInvoke(long traceId, String sourceInterfaceClass, String sourceId, String targetClass, String targetId, int methodId, Object[] params)
    {

    }

    default void postInvoke(long traceId, Object result)
    {

    }

}
