package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;

@SuppressWarnings("rawtypes")
public class SomeActorImpl extends AbstractActor implements SomeActor
{
    private UUID uniqueActivationId = UUID.randomUUID();
    private boolean activationWasCalled;
    private boolean canBeRemoved = true;

    @Override
    public Task<String> sayHello(final String greeting)
    {
        return Task.fromValue("bla");
    }

    @Override
    public Task<UUID> getUniqueActivationId()
    {
        return Task.fromValue(uniqueActivationId);
    }

    @Override
    public Task<UUID> getUniqueActivationId(final long sleepNanos)
    {
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
        return Task.fromValue(uniqueActivationId);
    }


    @Override
    public Task<Boolean> getActivationWasCalled()
    {
        return Task.fromValue(activationWasCalled);
    }

    public Task<?> activateAsync()
    {
        getLogger().debug("activateAsync");
        activationWasCalled = true;
        return super.activateAsync();
    }

    @Override
    public Task<?> deactivateAsync()
    {
        getLogger().debug("deactivateAsync");
        return super.deactivateAsync();
    }

    @Override
    public Task<String> getNodeId()
    {
        return Task.fromValue(runtimeIdentity());
    }

    @Override
    public Task setCanBeRemoved(final boolean canBeRemoved)
    {
        this.canBeRemoved = canBeRemoved;
        return Task.done();
    }

    @Override
    public Task<Boolean> canBeRemoved(final boolean ttlExpired)
    {
        if (!canBeRemoved)
        {
            return Task.fromValue(false);
        }
        return Task.fromValue(ttlExpired);
    }
}
