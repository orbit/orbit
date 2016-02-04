package com.ea.orbit.actors.peer.streams;


import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.concurrent.Task;

public interface ServerSideStreamProxy extends ActorObserver
{
    <T> Task<StreamSubscriptionHandle<T>> subscribe(String provider, int dataClassId, String streamId, ClientSideStreamProxy proxy);

    <T> Task<Void> unsubscribe(StreamSubscriptionHandle<T> handle);

}
