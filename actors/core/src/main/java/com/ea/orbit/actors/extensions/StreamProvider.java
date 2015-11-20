package com.ea.orbit.actors.extensions;

import com.ea.orbit.actors.streams.AsyncStream;

public interface StreamProvider extends ActorExtension
{
    <T> AsyncStream<T> getStream(Class<T> dataClass, String id);

    String getName();
}
