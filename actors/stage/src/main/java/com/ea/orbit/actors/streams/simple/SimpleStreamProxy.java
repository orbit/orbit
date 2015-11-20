package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.streams.AsyncObserver;
import com.ea.orbit.concurrent.Task;

public interface SimpleStreamProxy<T> extends ActorObserver, AsyncObserver<T>
{
    @Override
    Task<Void> onNext(T data);


    Task<Void> onError(Exception ex);
}
