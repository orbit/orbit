package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

public class SomePlayerActor extends AbstractActor<SomePlayerActor.SomePlayerStateDto> implements SomePlayer
{
    public static class SomePlayerStateDto
    {
        int matchEventCount;
    }

    @Override
    public Task<?> activateAsync()
    {
        getLogger().debug("activateAsync");
        return super.activateAsync();
    }

    @Override
    public Task<?> deactivateAsync()
    {
        getLogger().debug("deactivateAsync");
        return super.deactivateAsync();
    }

    @Override
    public Task<String> getName()
    {
        return null;
    }

    @Override
    public Task<Void> joinMatch(final SomeMatch someMatch)
    {
        return someMatch.addPlayer(this);
    }

    @Override
    public Task<Void> matchEvent(final SomeMatch someMatch)
    {
        state().matchEventCount++;
        return Task.fromValue(null);
    }

    @Override
    public Task<Integer> getMatchEventCount()
    {
        return Task.fromValue(state().matchEventCount);
    }


    @Override
    public Task<String> getNodeId()
    {
        return Task.fromValue(runtimeIdentity());
    }
}
