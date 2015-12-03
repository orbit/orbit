package com.ea.orbit.actors.streams.simple;

import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.cluster.NodeAddress;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;

import java.util.concurrent.ConcurrentHashMap;

import static com.ea.orbit.async.Await.await;

public class SimpleStreamActor extends AbstractActor<SimpleStreamActor.State> implements SimpleStream
{
    public static class State
    {
        ConcurrentHashMap<String, SimpleStreamProxy> subscribers = new ConcurrentHashMap<>();
    }

    @Override
    public Task<Void> unsubscribe(final String handle)
    {
        if (state().subscribers.remove(handle) != null)
        {
            writeState();
        }
        return Task.done();
    }

    @Override
    public Task<String> subscribe(final SimpleStreamProxy subscriber)
    {
        // better than using a count because the count may go backwards if the node fails.
        String handle = IdUtils.urlSafeString(96);
        state().subscribers.put(handle, subscriber);
        writeState();
        return Task.fromValue(handle);
    }

    @Override
    public Task<?> activateAsync()
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Activating stream: {}", actorIdentity());
        }
        await(super.activateAsync());

        int size = state().subscribers.size();

        // check if each subscriber is still alive.
        await(Task.allOf(state().subscribers.entrySet().stream()
                .map(entry -> checkAlive(entry.getKey(), entry.getValue()))));

        if (size != state().subscribers.size())
        {
            return writeState();
        }
        return Task.done();
    }

    private Task<Boolean> checkAlive(final String handle, final SimpleStreamProxy subscriber)
    {
        final ActorRuntime runtime = ActorRuntime.getRuntime();

        final NodeAddress r = await(runtime.locateActor((Addressable) subscriber, false));
        if (r == null)
        {
            state().subscribers.remove(handle);
            return Task.fromValue(Boolean.FALSE);
        }
        return Task.fromValue(Boolean.TRUE);
    }

    @Override
    public <T> Task<Void> publish(final T data)
    {
        return Task.allOf(state().subscribers.entrySet().stream()
                .map(entry -> entry.getValue().onNext(data, null)
                        .exceptionally(r -> {
                            checkAlive(entry.getKey(), entry.getValue());
                            return null;
                        })));
    }

}
