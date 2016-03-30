/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.time.Clock;

/**
 * Interface used by the generated code to interact with the orbit actors runtime.
 */
public interface BasicRuntime extends DescriptorFactory
{

    /**
     * Handles calls to actor reference methods.
     *
     * @param toReference destination actor reference or observer reference
     * @param oneWay      should expect an answer,
     *                    if false the task is completed with null.
     * @param methodId    the generated id for the method
     * @param params      the method parameters, must all be serializable.
     * @return a future with the return value, or a future with null (if one-way)
     */
    Task<?> invoke(RemoteReference toReference, Method m, boolean oneWay, final int methodId, final Object[] params);


    /**
     * Returns an actor reference
     */
    default <T> T getReference(final Class<T> iClass, final Object id)
    {
        return getReference(this, null, iClass, id);
    }

    /**
     * Installs this observer into this node.
     * Can called several times the object is registered only once.
     *
     * @param iClass   hint to the framework about which ActorObserver interface this object represents.
     *                 Can be null if there are no ambiguities.
     * @param observer the object to install
     * @param <T>      The type of reference class returned.
     * @return a remote reference that can be sent to actors, the runtime will chose one id
     */
    default <T extends ActorObserver> T registerObserver(final Class<T> iClass, final T observer)
    {
        return registerObserver(iClass, null, observer);
    }

    /**
     * Installs this observer into this node.
     * Can called several times the object is registered only once.
     *
     * @param iClass   hint to the framework about which ActorObserver interface this object represents.
     *                 Can be null if there are no ambiguities.
     * @param id       the observer id
     * @param observer the object to install
     * @param <T>      The type of reference class returned.
     * @return a remote reference that can be sent to actors.
     */
    <T extends ActorObserver> T registerObserver(Class<T> iClass, String id, T observer);


    /**
     * Returns an observer reference to an observer in another node.
     * <p/>
     * Should only be used if the application knows for sure that an observer with the given id
     * indeed exists on that other node.
     * <p/>
     * This is a low level use of orbit-actors, recommended only for ActorExtensions.
     *
     * @param address the other node address.
     * @param iClass  the IObserverClass
     * @param id      the id, must not be null
     * @param <T>     the ActorObserver sub interface
     * @return a remote reference to the observer
     */
    default <T extends ActorObserver> T getRemoteObserverReference(NodeAddress address, final Class<T> iClass, final Object id)
    {
        return getReference(this, address, iClass, id);
    }

    /**
     * Sets a static reference to the last created runtime.
     *
     * @param runtimeRef a reference to the runtime
     */
    static void setRuntime(final WeakReference<? extends BasicRuntime> runtimeRef)
    {
        RuntimeBinder.setRuntime(runtimeRef);
    }

    static BasicRuntime getRuntime()
    {
        return RuntimeBinder.getBasicRuntime();
    }

    <T> AsyncStream<T> getStream(String provider, Class<T> dataClass, String id);

    default <T> AsyncStream<T> getStream(final Class<T> dataClass, final String streamId)
    {
        return getStream(AsyncStream.DEFAULT_PROVIDER, dataClass, streamId);
    }

    /**
     * Gets the local clock. It's usually the system clock, but it can be changed for testing.
     *
     * @return the clock that should be used for checking the time during tests.
     */
    Clock clock();

    default Logger getLogger(Object target)
    {
        return (target instanceof Class) ? LoggerFactory.getLogger((Class) target)
                : (target instanceof String) ? LoggerFactory.getLogger((String) target)
                : (target != null) ? LoggerFactory.getLogger(target.getClass())
                : LoggerFactory.getLogger("root");
    }

    void bind();
}
