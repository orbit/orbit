package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;

public class SomePlayer extends OrbitActor<SomePlayer.SomePlayerStateDto> implements ISomePlayer
{
    public static class SomePlayerStateDto
    {
        int matchEventCount;
    }

    @Override
    public Task<String> getName()
    {
        return null;
    }

    @Override
    public Task<Void> joinMatch(final ISomeMatch someMatch)
    {
        return someMatch.addPlayer(this);
    }

    @Override
    public Task<Void> matchEvent(final ISomeMatch someMatch)
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
