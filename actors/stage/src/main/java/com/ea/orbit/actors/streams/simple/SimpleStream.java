package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.Task;


/**
 * Internal implementation of of the simple stream provider.
 * This class should not be used directly by application code
 */
public interface SimpleStream extends Actor
{
    Task<Void> unsubscribe(String handle);

    <T> Task<String> subscribe(SimpleStreamProxy<T> subscriber);

    <T> Task<Void> publish(T data);
}
