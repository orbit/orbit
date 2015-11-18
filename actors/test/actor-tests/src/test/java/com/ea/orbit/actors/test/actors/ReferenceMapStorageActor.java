package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

import static com.ea.orbit.async.Await.await;
/**
 * Created by jefft on 11/18/2015.
 */
public class ReferenceMapStorageActor extends AbstractActor<ReferenceMapStorageActor.State> implements ReferenceMapStorage
{
    public static class State
    {
        Map<Integer, SomePlayer> playerMap = new HashMap<>();
    }

    @Override
    public Task<Void> addPlayerToMap(SomePlayer player)
    {
        state().playerMap.put(player.hashCode(), player);
        await(writeState());
        return Task.done();
    }

    @Override
    public Task<Map<Integer, SomePlayer>> getPlayers()
    {
        return Task.fromValue(state().playerMap);
    }
}
