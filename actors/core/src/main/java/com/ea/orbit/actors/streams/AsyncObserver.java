package com.ea.orbit.actors.streams;


import com.ea.orbit.concurrent.Task;

public interface AsyncObserver<T>
{
    Task<Void> onNext(T data, final StreamSequenceToken sequenceToken);

    default Task<Void> onError(Exception ex)
    {
        return Task.done();
    }
}
