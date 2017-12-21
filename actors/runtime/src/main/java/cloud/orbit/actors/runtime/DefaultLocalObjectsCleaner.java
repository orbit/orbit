/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.annotation.NeverDeactivate;
import cloud.orbit.actors.annotation.TimeToLive;
import cloud.orbit.actors.concurrent.ConcurrentExecutionQueue;
import cloud.orbit.actors.extensions.ActorDeactivationExtension;
import cloud.orbit.concurrent.Task;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ea.async.Async.await;

/**
 * Created by joeh on 2017-05-10.
 */
public class DefaultLocalObjectsCleaner implements LocalObjectsCleaner
{
    private final Logger logger = LoggerFactory.getLogger(DefaultLocalObjectsCleaner.class);

    private final LocalObjects localObjects;
    private final long defaultActorTTL;
    private final long deactivationTimeoutMillis;
    private final Clock clock;
    private final Hosting hosting;
    private final Set<ActorBaseEntry<?>> pendingDeactivations = ConcurrentHashMap.newKeySet();
    private final ConcurrentExecutionQueue concurrentExecutionQueue;
    private List<ActorDeactivationExtension> actorDeactivationExtensions = Collections.emptyList();

    public DefaultLocalObjectsCleaner(final Hosting hosting, final Clock clock, final ExecutorService executor, final LocalObjects localObjects, final long defaultActorTTL, final int concurrentDeactivations, final long deactivationTimeoutMillis)
    {
        this.hosting = hosting;
        this.clock = clock;
        this.localObjects = localObjects;
        this.defaultActorTTL = defaultActorTTL;
        this.deactivationTimeoutMillis = deactivationTimeoutMillis;
        this.concurrentExecutionQueue = new ConcurrentExecutionQueue(executor, concurrentDeactivations, 0);
    }

    @Override
    public Task deactivateActor(final Actor actor) {
        final LocalObjects.LocalObjectEntry<?> actorRef = localObjects.findLocalActor(actor);
        if(actorRef != null) {
            return deactivateEntry((ActorBaseEntry<?>)actorRef);
        }
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Task cleanup()
    {
        await(cleanupObservers());
        await(cleanupActors(false));
        return Task.done();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Task shutdown()
    {
        return cleanupActors(true);
    }

    private Task cleanupActors(final boolean shutdownAll)
    {
        final List<Map.Entry<Object, LocalObjects.LocalObjectEntry>> actorEntries = localObjects.stream()
                .filter(e -> e.getValue() instanceof ActorBaseEntry)
                .collect(Collectors.toList());

        final Collection<ActorBaseEntry<?>> baseEntries = actorEntries.stream()
                .map(entryEntry -> (ActorBaseEntry<?>) entryEntry.getValue())
                .collect(Collectors.toList());

        final Set<ActorBaseEntry<?>> toRemove = new HashSet<>();

        actorDeactivationExtensions.forEach(x -> x.cleanupActors(baseEntries, toRemove));

        final List<Task<Void>> pendingThisCycle = new ArrayList<>();

        for(final Map.Entry<Object, LocalObjects.LocalObjectEntry> entryEntry : actorEntries)
        {
            final ActorBaseEntry<?> actorEntry = (ActorBaseEntry<?>) entryEntry.getValue();

            if (actorEntry.isDeactivated())
            {
                // this might happen if the deactivation is called outside this loop,
                // for instance by the stateless worker that owns the objects
                localObjects.remove(entryEntry.getKey(), entryEntry.getValue());
                continue;
            }

            if (shutdownAll || shouldRemove(actorEntry, toRemove))
            {
                if (pendingDeactivations.add(actorEntry))
                {
                    pendingThisCycle.add(deactivateEntry(actorEntry));
                }
            }
        }

        return Task.allOf(pendingThisCycle);
    }

    private Task deactivateEntry(final ActorBaseEntry<?> actorEntry) {
        return concurrentExecutionQueue.execute(() ->
                actorEntry.deactivate().failAfter(deactivationTimeoutMillis, TimeUnit.MILLISECONDS)
                        .whenComplete((r, e) ->
                        {
                            // ensures removal
                            if (e != null)
                            {
                                // error occurred
                                if (logger.isErrorEnabled())
                                {
                                    logger.error("Error during the deactivation of " + actorEntry.getRemoteReference(), e);
                                }
                                // forcefully setting the entry to deactivated
                                actorEntry.setDeactivated(true);
                            }

                            try
                            {
                                localObjects.remove(actorEntry.reference, actorEntry);
                                if (actorEntry.reference == actorEntry.getRemoteReference())
                                {
                                    // removing non stateless actor from the distributed directory
                                    hosting.actorDeactivated(actorEntry.getRemoteReference());
                                }
                            }
                            finally
                            {
                                pendingDeactivations.remove(actorEntry);
                            }
                        })
        );
    }

    private boolean shouldRemove(final ActorBaseEntry<?> actorEntry, final Set<ActorBaseEntry<?>> toRemove)
    {
        final Class<?> interfaceClass = RemoteReference.getInterfaceClass(actorEntry.getRemoteReference());
        // Make sure it isn't tagged NeverDeactivate
        if (interfaceClass.isAnnotationPresent(NeverDeactivate.class))
        {
            return false;
        }

        // Check for ttl override
        if(interfaceClass.isAnnotationPresent(TimeToLive.class))
        {
            final TimeToLive customTtl = interfaceClass.getAnnotation(TimeToLive.class);
            final long customTtlMilliseconds = customTtl.timeUnit().toMillis(customTtl.value());
            if(clock.millis() - actorEntry.getLastAccess() > customTtlMilliseconds)
            {
                return true;
            }
        }
        else
            {
            // Check against default
            if(clock.millis() - actorEntry.getLastAccess() > defaultActorTTL)
            {
                return true;
            }
        }


        // Check if extension wanted to remove it
        if(toRemove.contains(actorEntry)) {
            return true;
        }

        return false;
    }

    private Task cleanupObservers()
    {
        localObjects.stream()
                .filter(e -> e.getValue() instanceof ObserverEntry && e.getValue().getObject() == null)
                .forEach(e -> localObjects.remove(e.getKey(), e.getValue()));
        return Task.done();
    }

    @Override
    public void setActorDeactivationExtensions(final List<ActorDeactivationExtension> extensionList)
    {
        this.actorDeactivationExtensions = Collections.unmodifiableList(extensionList);
    }

}
