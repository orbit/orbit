package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import java.util.ArrayList;
import java.util.List;

public class SomeMatchActor extends AbstractActor<SomeMatchActor.SomeMatchDto> implements SomeMatch
{
    public static class SomeMatchDto
    {
        private List<SomePlayer> players = new ArrayList<>();

        public List<SomePlayer> getPlayers()
        {
            return players;
        }

        public void setPlayers(final List<SomePlayer> players)
        {
            this.players = players;
        }
    }

    @Override
    public Task<Void> addPlayer(final SomePlayer player)
    {
        state().players.add(player);
        return writeState();
    }

    @Override
    public Task<List<SomePlayer>> getPlayers()
    {
        return Task.fromValue(state().getPlayers());
    }

    @Override
    public Task<String> getNodeId()
    {
        return Task.fromValue(runtimeIdentity());
    }

    @Override
    public Task<?> deactivateAsync()
    {
        for (SomePlayer p : state().players)
        {
            try
            {
                p.matchEvent(this).get();
            }
            catch (Exception e)
            {
                throw new UncheckedException(e);
            }
        }
        return super.deactivateAsync();
    }
}
