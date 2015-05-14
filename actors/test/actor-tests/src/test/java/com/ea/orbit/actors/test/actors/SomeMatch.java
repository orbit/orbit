package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import java.util.ArrayList;
import java.util.List;

public class SomeMatch extends AbstractActor<SomeMatch.SomeMatchDto> implements ISomeMatch
{
    public static class SomeMatchDto
    {
        private List<ISomePlayer> players = new ArrayList<>();

        public List<ISomePlayer> getPlayers()
        {
            return players;
        }

        public void setPlayers(final List<ISomePlayer> players)
        {
            this.players = players;
        }
    }

    @Override
    public Task<Void> addPlayer(final ISomePlayer player)
    {
        state().players.add(player);
        return writeState();
    }

    @Override
    public Task<List<ISomePlayer>> getPlayers()
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
        for (ISomePlayer p : state().players)
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
