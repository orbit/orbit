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

package cloud.orbit.actors;

import cloud.orbit.actors.annotation.NoIdentity;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.actors.runtime.RuntimeActions;
import cloud.orbit.concurrent.Task;
import cloud.orbit.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.ea.async.Async.await;

/**
 * Interface marker for orbit actors.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 * public interface Hello extends Actor
 * {
 *      Task<String> sayHello();
 * }
 * <p/>
 * public class HelloActor extends AbstractActor implements Hello
 * {
 *     public Task<String> sayHello() {
 *         return Task.fromValue("hello!");
 *     }
 * }
 * }</pre>
 * </p>
 * <p>
 * The presence of the Actor interface instructs the java compiler to
 * generate a reference factory class for that interface.
 * </p><p>
 * Application code will never touch actor instances directly. It should rather
 * obtain references:
 * <pre>{@code
 *  HelloActor helloActor = Actor.getReference(Hello.class, "001");
 * }</pre>
 * </p>
 */
public interface Actor
{
    /**
     * Gets a reference to an actor.
     *
     * @param actorInterface the actor interface
     * @param id             the actor id
     * @param <T>            the interface type
     * @return an actor reference
     */
    static <T extends Actor> T getReference(Class<T> actorInterface, String id)
    {
        if (actorInterface.isAnnotationPresent(NoIdentity.class))
        {
            throw new IllegalArgumentException("Shouldn't supply ids for Actors annotated with " + NoIdentity.class);
        }
        else if(StringUtils.isBlank(id))
        {
            throw new IllegalArgumentException("Actor ids may not be null or \"\".");
        }
        return DefaultDescriptorFactory.ref(actorInterface, id);
    }

    /**
     * Gets a reference to an actor that has the {@literal@}NoIdentity annotation.
     *
     * @param actorInterface the actor interface
     * @param <T>            the interface type
     * @return an actor reference
     */
    static <T extends Actor> T getReference(Class<T> actorInterface)
    {
        if (!actorInterface.isAnnotationPresent(NoIdentity.class))
        {
            throw new IllegalArgumentException("Not annotated with " + NoIdentity.class);
        }
        return DefaultDescriptorFactory.ref(actorInterface, null);
    }

    /**
     * Gets a the id of an actor reference or instance.
     *
     * @param actor the actor whose identity you wish to retrieve
     * @return the actor's identity
     */
    static String getIdentity(Actor actor)
    {
        return String.valueOf(RemoteReference.getId(RemoteReference.from(actor)));
    }

    /**
     * Requests the deactivation of an actor
     *
     * @param actor the actor which you want to deactivate.
     * @return A task indicating the state of the request. Immediately resolved if actor is not activated.
     */
    static Task deactivate(final Actor actor)
    {
        final NodeAddress address = await(ActorRuntime.getRuntime().locateActor(RemoteReference.from(actor), false));
        if(address != null) {
            final RuntimeActions runtimeActions = DefaultDescriptorFactory.observerRef(address, RuntimeActions.class, "");
            return runtimeActions.deactivateActor(actor);
        }

        return Task.done();
    }

    /**
     * Requests the global actor count across the cluster.
     *
     * @return A task indicating containing the total actor count across the cluster.
     */
    static Task<Long> getClusterActorCount()
    {
        final List<Task<Long>> countList = ActorRuntime.getRuntime().getAllNodes().stream()
                .map(address ->
                {
                    final RuntimeActions runtimeActions = DefaultDescriptorFactory.observerRef(address, RuntimeActions.class, "");
                    return runtimeActions.getActorCount();
                })
                .collect(Collectors.toList());

        await(Task.allOf(countList));

        final Long actorCount = countList.stream()
                .mapToLong(Task<Long>::join)
                .sum();

        return Task.fromValue(actorCount);
    }


    /**
     * Gets a the id of the current actor reference or instance.
     *
     * @return the actor's identity
     */
    default String getIdentity()
    {
        return Actor.getIdentity(this);
    }

    static <T> T cast(Class<T> remoteInterface, Actor actor)
    {
        return DefaultDescriptorFactory.cast(remoteInterface, actor);
    }

}
