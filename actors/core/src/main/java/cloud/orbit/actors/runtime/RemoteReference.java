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

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Addressable;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.concurrent.Task;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Base class for Actor or ActorObserver references.
 *
 * <p>References classes ares automatically generated with runtime byte code generation</p>
 *
 * <p>The reference classes extends the Actor or ActorObserver interfaces
 * and implement their methods as remote calls that are forwarded to the IRuntime</p>
 *
 * <p>References are java.io.Serializable and orbit also provides a jackson module to handle their serialization to json</p>
 *
 * @param <T> the Actor of ActorObserver implemented by this reference.
 */
public abstract class RemoteReference<T> implements Serializable, Addressable
{
    private static final long serialVersionUID = 1L;

    NodeAddress address;
    Object id;
    transient BasicRuntime runtime;


    /**
     * Implemented by subclasses to return the interface id.
     *
     * <p>This is not exposed as a public method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @return the implemented interface id
     */
    protected abstract int _interfaceId();

    /**
     * Implemented by subclasses to return the actor or actor observer interface implemented by this reference.
     *
     * @return the implemented interface
     */
    protected abstract Class<T> _interfaceClass();

    /**
     * Actor reference constructor called by subclasses.
     *
     * @param id the actor or actor observer id
     */
    public RemoteReference(final Object id)
    {
        this.id = id;
        if (id == null)
        {
            throw new IllegalArgumentException("Ids must not be null");
        }
    }

    /**
     * Invokes a remote method.
     *
     * @param oneWay   if the target should send the response back.
     * @param methodId the target method id
     * @param params   parameters for the method, must all be serializable.
     * @param <R>      generic parameter to make the compiler happy.
     * @return a task that will contain the returned value, or if one-way a task indicating if the message could be send.
     */
    @SuppressWarnings("unchecked")
    protected <R> Task<R> invoke(final Method method, final boolean oneWay, final int methodId, final Object[] params)
    {
        return (Task<R>) (runtime != null ? runtime : BasicRuntime.getRuntime()).invoke(this, method, oneWay, methodId, params);
    }

    @Override
    public boolean equals(final Object o)
    {
        RemoteReference<?> that;
        return (this == o) || ((o instanceof RemoteReference)
                && ((_interfaceId() == (that = (RemoteReference<?>) o)._interfaceId())
                && ((!(address != null ? !address.equals(that.address) : that.address != null))
                && ((!(id != null ? !id.equals(that.id) : that.id != null))))));
    }

    @Override
    public int hashCode()
    {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + _interfaceId();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    /**
     * Utility method used by the framework and framework extensions to retrieve the integer identifier of
     * the Actor or ActorObserver interface implemented by this reference.
     *
     * <p>This is not exposed as an instance method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @param reference the reference being inspected
     * @return the implemented interface id
     */
    public static int getInterfaceId(final RemoteReference<?> reference)
    {
        return reference._interfaceId();
    }

    /**
     * Utility method used by the framework and framework extensions to retrieve the object from the references.
     * <p>This is not exposed as an instance method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @param reference the reference being inspected
     * @return the actor id
     */
    public static Object getId(final RemoteReference<?> reference)
    {
        return reference.id;
    }

    public static Object getId(final AbstractActor actor)
    {
        return getId(from(actor));
    }

    /**
     * Utility method used by the framework and framework extensions to retrieve the Actor or ActorObserver
     * interface implemented by this reference.
     *
     * <p>This is not exposed as an instance method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @param reference the reference being inspected
     * @return the implemented interface
     */
    public static <R> Class<R> getInterfaceClass(final RemoteReference<R> reference)
    {
        return reference._interfaceClass();
    }

    @SuppressWarnings("unchecked")
    public static <R> Class<R> getInterfaceClass(final AbstractActor reference)
    {
        return from(reference)._interfaceClass();
    }

    /**
     * Utility method used by the framework and framework extensions to retrieve the target node address
     * for this reference. The target address will be null for regular actors and non null for actor observers.
     *
     * <p>This is not exposed as an instance method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @param reference the reference being inspected
     * @return the node address where the actor observer resides
     */
    public static NodeAddress getAddress(final RemoteReference<?> reference)
    {
        return reference.address;
    }

    /**
     * Utility method used by the framework and framework extensions to set the target node address
     * for this reference.
     *
     * <p>This is not exposed as an instance method to avoid clashes with the methods from implemented interfaces</p>
     *
     * @param reference   the reference being inspected
     * @param nodeAddress the node address where the actor observer resides
     */
    public static void setAddress(final RemoteReference<?> reference, final NodeAddress nodeAddress)
    {
        reference.address = nodeAddress;
    }

    public static RemoteReference from(AbstractActor actor)
    {
        return actor.reference;
    }

    public static RemoteReference from(Actor actor)
    {
        return actor instanceof AbstractActor ? ((AbstractActor) actor).reference
                : actor instanceof RemoteReference ? (RemoteReference) actor
                : null;
    }

    @Override
    public String toString()
    {
        if (address == null)
        {
            return id != null ? _interfaceClass().getName() + ":" + id : _interfaceClass().getName();
        }
        return id != null ? _interfaceClass().getName() + ":" + id : _interfaceClass().getName() + ":" + address;
    }

    public static void setRuntime(final RemoteReference<?> reference, final BasicRuntime runtime)
    {
        reference.runtime = runtime;
    }

    public static BasicRuntime getRuntime(final RemoteReference<?> original)
    {
        return original.runtime;
    }
}
