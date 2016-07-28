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

import cloud.orbit.actors.Addressable;
import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.extensions.StreamProvider;
import cloud.orbit.concurrent.Task;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Interface used by the generated code to interact with the orbit actors runtime.
 */
public interface ActorRuntime extends BasicRuntime
{

    /**
     * Registers a timer for the orbit actor
     *
     * @param actor        the actor requesting the timer.
     * @param taskCallable a callable that must return a task.
     * @param dueTime      the first time the timer will tick.
     * @param period       the period of subsequent ticks (if ZERO then will only tick once)
     * @param timeUnit     the time unit for period and dueTime
     * @return a registration that allows to cancel the timer.
     */
    Registration registerTimer(AbstractActor<?> actor, Callable<Task<?>> taskCallable, long dueTime, long period, TimeUnit timeUnit);

    /**
     * Registers or updated a persisted reminder.
     *
     * @param actor        the reference to the actor.
     * @param reminderName the remainder's name
     * @param dueTime      how long since now the first tick should be triggered.
     * @param period       after the first tick, how often should the reminder be called.
     * @param timeUnit     the time unit for dueTime and period
     * @return completion promise for this operation
     */
    Task<?> registerReminder(Remindable actor, String reminderName, long dueTime, long period, TimeUnit timeUnit);

    /**
     * Removes a previously registered reminder.
     *
     * @param actor        the actor that registered this reminder
     * @param reminderName the remainder's name
     * @return completion promise for this operation
     */
    Task<?> unregisterReminder(Remindable actor, String reminderName);


    /**
     * Locates the node address of an actor.
     *
     * @param forceActivation a node will be chosen to activate the actor if
     *                        it's not currently active.
     *                        Actual activation is postponed until the actor receives one message.
     * @return actor address, null if actor is not active and forceActivation==false
     */
    Task<NodeAddress> locateActor(final Addressable actorReference, final boolean forceActivation);

    /**
     * Gets the current node address in the cluster.
     *
     * @return the current node address
     */
    NodeAddress getLocalAddress();


    /**
     * Gets a string that represents uniquely the node that currently holds this actor.
     *
     * @return unique identity string
     */
    String runtimeIdentity();


    void bind();


    StreamProvider getStreamProvider(String name);

    /**
     * Sets a static reference to the last created runtime.
     *
     * @param runtimeRef a reference to the runtime
     */
    static void setRuntime(final WeakReference<ActorRuntime> runtimeRef)
    {
        RuntimeBinder.setRuntime(runtimeRef);
    }

    static ActorRuntime getRuntime()
    {
        return RuntimeBinder.getActorRuntime();
    }

    List<ActorExtension> getExtensions();

    @SuppressWarnings("unchecked")
    default <T extends ActorExtension> List<T> getAllExtensions(Class<T> itemType)
    {
        final List<ActorExtension> extensions = getExtensions();
        if (extensions == null || extensions.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        for (ActorExtension extension : extensions)
        {
            if (itemType.isInstance(extension)) {
                result.add((T) extension);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    default <T extends ActorExtension> T getFirstExtension(Class<T> itemType)
    {
        final List<ActorExtension> extensions = getExtensions();
        if (extensions == null || extensions.isEmpty()) {
            return null;
        }
        for (ActorExtension extension : extensions) {
            if (itemType.isInstance(extension)) {
                return (T) extension;
            }
        }
        return null;
    }

    List<NodeAddress> getAllNodes();

}
