package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
public class StatelessThingActor extends AbstractActor implements StatelessThing
{
    private UUID uuid = UUID.randomUUID();
    static AtomicInteger activationCount = new AtomicInteger();
    private String activationId = String.valueOf(activationCount.incrementAndGet());


    @Override
    public Task<String> sayHello()
    {
        Thread.yield();
        return Task.fromValue("hello from " + actorIdentity() + "/" + activationId);
    }

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
