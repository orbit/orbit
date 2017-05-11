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

import cloud.orbit.actors.annotation.NeverDeactivate;
import cloud.orbit.actors.concurrent.ConcurrentExecutionQueue;
import cloud.orbit.concurrent.ConcurrentHashSet;
import cloud.orbit.concurrent.Task;

import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;

/**
 * Created by joeh on 2017-05-10.
 */
public class LocalObjectsCleaner
{
    private Logger logger = LoggerFactory.getLogger(LocalObjectsCleaner.class);

    final LocalObjects localObjects;
    final long defaultActorTTL;
    final long deactivationTimeoutMillis;
    final Clock clock;
    final Hosting hosting;
    final ConcurrentHashSet<ActorBaseEntry<?>> pendingDeactivations = new ConcurrentHashSet<>();

    final ConcurrentExecutionQueue concurrentExecutionQueue;


    public LocalObjectsCleaner(final Hosting hosting, final Clock clock, final ExecutorService executor, final LocalObjects localObjects, final long defaultActorTTL, final int concurrentDeactivations, final long deactivationTimeoutMillis)
    {
        this.hosting = hosting;
        this.clock = clock;
        this.localObjects = localObjects;
        this.defaultActorTTL = defaultActorTTL;
        this.deactivationTimeoutMillis = deactivationTimeoutMillis;
        this.concurrentExecutionQueue = new ConcurrentExecutionQueue(executor, concurrentDeactivations, 0);
    }

    public Task cleanup()
    {
        await(cleanupObservers());
        await(cleanupActors(false));
        return Task.done();
    }

    public Task shutdown()
    {
        await(cleanupActors(true));
        return Task.done();
    }

    public boolean hasBacklog()
    {
        return concurrentExecutionQueue.hasBacklog();
    }

    private Task cleanupActors(final boolean shutdownAll)
    {
        final Iterator<Map.Entry<Object, LocalObjects.LocalObjectEntry>> iterator = localObjects.stream()
                .filter(e -> e.getValue() instanceof ActorBaseEntry)
                .iterator();

        while (iterator.hasNext())
        {
            final Map.Entry<Object, LocalObjects.LocalObjectEntry> entryEntry = iterator.next();
            final ActorBaseEntry<?> actorEntry = (ActorBaseEntry<?>) entryEntry.getValue();

            if (actorEntry.isDeactivated())
            {
                // this might happen if the deactivation is called outside this loop,
                // for instance by the stateless worker that owns the objects
                localObjects.remove(entryEntry.getKey(), entryEntry.getValue());
                continue;
            }

            boolean shouldRemove;
            // Check for timeout
            shouldRemove = clock.millis() - actorEntry.getLastAccess() > defaultActorTTL;
            // Make sure it isn't tagged NeverDeactivate
            shouldRemove = shouldRemove && !RemoteReference.getInterfaceClass(actorEntry.getRemoteReference()).isAnnotationPresent(NeverDeactivate.class);
            // Override if shutdownAll is true
            shouldRemove = shouldRemove || shutdownAll;


            if (shouldRemove)
            {
                if (pendingDeactivations.add(actorEntry))
                {
                    concurrentExecutionQueue.execute(() ->
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

                                        pendingDeactivations.remove(actorEntry);
                                        localObjects.remove(entryEntry.getKey(), entryEntry.getValue());
                                        if (entryEntry.getKey() == actorEntry.getRemoteReference())
                                        {
                                            // removing non stateless actor from the distributed directory
                                            hosting.actorDeactivated(actorEntry.getRemoteReference());
                                        }
                                    }));
                }
            }
        }

        return Task.done();
    }

    private Task cleanupObservers()
    {
        localObjects.stream()
                .filter(e -> e.getValue() instanceof ObserverEntry && e.getValue().getObject() == null)
                .forEach(e -> localObjects.remove(e.getKey(), e.getValue()));
        return Task.done();
    }

}
