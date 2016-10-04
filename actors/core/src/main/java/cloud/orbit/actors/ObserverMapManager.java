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
import cloud.orbit.concurrent.Task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * {@link ObserverMapManager} is an optional thread safe "map" that can be used by actors which need to
 * verify if an {@link ActorObserver} associated to another object eg. player is still in the collection of observers.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class ObserverMapManager<K, V extends ActorObserver> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ObserverMapManager.class);

    private final ConcurrentHashMap<K, V> observers = new ConcurrentHashMap<>();

    public Task<?> cleanup() {
        Stream stream = this.observers.entrySet().stream().map(entry -> {
            ActorObserver observer = entry.getValue();
            return (observer.ping()).whenComplete((Object result, Throwable throwable) -> {
                if (throwable != null) {
                    remove(entry.getKey());
                }
            });
        });
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
    public void notifyObservers(final Consumer<V> callable) {
        List<V> fail = null;
        for (V observer : observers.values())
        {
            try {
                callable.accept(observer);
            } catch (final Exception ex) {
                if (fail == null) {
                    fail = new ArrayList<>();
                }
                fail.add(observer);
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing observer due to exception", ex);
                }
            }
        }
        if (fail != null && fail.size() > 0)
        {
            observers.values().removeAll(fail);
        }
    }

    /**
     * Used to notify the observers (call some method of their remote interface)
     * <p>
     * <pre>
     * String message = "...";
     * observers.notifyObservers((k, o) -&gt; o.receiveMessage(message));
     * </pre>
     * </p>
     *
     * @param callable operation to be called on all observers
     */
    public void notifyObservers(final BiConsumer<K, V> callable) {
        List<V> fail = null;
        for (Map.Entry<K, V> entry : observers.entrySet()) {
            try {
                callable.accept(entry.getKey(), entry.getValue());
            } catch (final Exception ex) {
                if (fail == null) {
                    fail = new ArrayList<>();
                }
                fail.add(entry.getValue());
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing observer due to exception", ex);
                }
            }
        }
        if (fail != null && fail.size() > 0) {
            observers.values().removeAll(fail);
        }
    }

    public V remove(K key) {
        return this.observers.remove(key);
    }

    public V get(K key) {
        return this.observers.get(key);
    }

    public V put(K key, V observer) {
        if (key == null) {
            throw new NullPointerException("Key must not be null");
        }
        if (observer == null) {
            throw new NullPointerException("Observer must not be null");
        }

        if (!(observer instanceof RemoteReference)) {
            throw new IllegalArgumentException("Was expecting a reference");
        }
        return this.observers.put(key, observer);
    }

    public void forEach(BiConsumer<K, V> callable) {
        this.observers.forEach(callable);
    }

    public boolean containsKey(K key) {
        return this.observers.containsKey(key);
    }

    public int size() {
        return this.observers.size();
    }

    public Stream<V> stream() {
        return this.observers.values().stream();
    }
}
