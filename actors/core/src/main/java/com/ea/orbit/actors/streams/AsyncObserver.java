package com.ea.orbit.actors.streams;


import com.ea.orbit.concurrent.Task;

public interface AsyncObserver<T>
{
    Task<Void> onNext(T data);

    default Task<Void> onError(Exception ex)
    {
        // ignore
        return Task.done();
    }
}
