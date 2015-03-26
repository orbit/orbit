package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;

public class StatelessThing extends OrbitActor implements IStatelessThing
{
    private UUID uuid = UUID.randomUUID();

    @Override
    public Task<UUID> getUniqueActivationId()
    {
        // just to ensure some parallelism;
        Thread.yield();
        return Task.fromValue(uuid);
    }

    @Override
    public Task<UUID> getUniqueActivationId(final long sleepNanos)
    {
        // just to ensure some parallelism;
        Thread.yield();
        long start = System.nanoTime();
        if (sleepNanos >= 1000)
        {
            try
            {
                Thread.sleep(sleepNanos / 1000);
            }
            catch (InterruptedException e)
            {
                getLogger().error("Error sleeping", e);
            }
        }
        while (start + sleepNanos >= System.nanoTime())
        {
            // do nothing, waiting.
        }
        return Task.fromValue(uuid);
    }
}
