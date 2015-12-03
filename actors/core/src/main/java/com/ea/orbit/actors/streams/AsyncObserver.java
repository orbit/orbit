package com.ea.orbit.actors.streams;


import com.ea.orbit.concurrent.Task;

public interface AsyncObserver<T>
{
    /// add sequence token
    Task<Void> onNext(T data, final StreamSequenceToken sequenceToken);

    default Task<Void> onError(Exception ex)
    {
        // ignore
        return Task.done();
    }
}
