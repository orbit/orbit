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

import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.ConcurrentHashSet;
import cloud.orbit.concurrent.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * ObserverManager is an optional thread safe collection that can be used by actors which need to call observers.
 * ObserverManager can be persisted and send to other actors.
 * <p>Its principal utility method is the {@code cleanup()} which asynchronously removes dead observers.
 *
 * @param <T> observable type.
 */
public class ObserverManager<T extends ActorObserver> implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ObserverManager.class);

    private final ConcurrentHashSet<T> observers = new ConcurrentHashSet<>();

    /**
     * Ensures that this collection contains the specified observer.
     *
     * @param observer observer whose presence in this collection is to be ensured
     * @return <tt>true</tt> if this collection changed as a result of the call.
     */
    public boolean addObserver(final T observer)
    {
        if (observer == null)
        {
            throw new NullPointerException("Observer must not be null");
        }

        if (!(observer instanceof RemoteReference))
        {
            throw new IllegalArgumentException("Was expecting a reference");
        }

        return observers.add(observer);
    }

    /**
     * Pings all observers and removes the ones that no longer exist or that return an exception.
     * <p>
     * This can take a while as some requests may timeout if the node that was holding the observer has left the cluster.
     * </p>
     * <p>
     * The observer set can handle concurrent modifications.<br/>
     * So it is not necessary, nor recommended, to wait on the returned Task unless the application really needs it.
     * </p>
     * Recommended usage:
     * <pre><code>
     * public Task activateAsync()
     * {
     *     await(super.activateAsync());
     *     // intentionally not waiting for the cleanup
     *     state().observers.cleanup();
     *     return Task.done();
     * }
     * </code></pre>
     *
     * @return a task that will be completed when the cleanup process is finished.
     * It's not recommended to wait on this task since it might take a while to finish the
     * cleanup process and ObserverManager is thread safe.
     */
    public Task<?> cleanup()
    {
        // TODO: replace ping for a single batch call for each node containing observers from this list.
        // TODO: add a observer validation function to the runtime. ActorRuntime
        final Stream<Task<?>> stream = observers.stream()
                .map(o ->
                        ((Task<?>) (o).ping()).whenComplete((final Object pr, final Throwable pe) ->
                        {
                            if (pe != null)
                            {
                                // beware: this might run in parallel with other calls the actor.
                                // this shouldn't be a problem.
                                observers.remove(o);
                            }
                        }));
        return Task.allOf(stream);
    }

    /**
     * Used to notify the observers (call some method of their remote interface)
     * <p>
     * <pre>
     * String message = "...";
     * observers.notifyObservers(o -&gt; o.receiveMessage(message));
     * </pre>
     * </p>
     *
     * @param callable operation to be called on all observers
     */
    public void notifyObservers(final Consumer<T> callable)
    {
        List<T> fail = null;
        for (T observer : observers)
        {
            try
            {
                callable.accept(observer);
            }
            catch (final Exception ex)
            {
                if (fail == null) {
                    fail = new ArrayList<>();
                }
                fail.add(observer);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Removing observer due to exception", ex);
                }
            }
        }
        if (fail != null && fail.size() > 0)
        {
            observers.removeAll(fail);
        }
    }

    /**
     * Remove all observers
     */
    public void clear()
    {
        observers.clear();
    }

    /**
     * Remove one observer
     *
     * @param observer observer to be removed
     */
    public void removeObserver(final T observer)
    {
        observers.remove(observer);
    }

    /**
     * Returns whether any observers are registered with this manager.
     */
    public boolean isEmpty() {
        return observers.isEmpty();
    }

    /**
     * Returns a sequential {@code Stream} with this collection as its source.
     *
     * @return a sequential {@code Stream} over the elements in this collection.
     */
    public Stream<T> stream()
    {
        return observers.stream();
    }
}
