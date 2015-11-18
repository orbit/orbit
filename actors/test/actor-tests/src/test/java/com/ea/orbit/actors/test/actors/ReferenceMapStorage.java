package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.Task;

import java.util.Map;

/**
 * Created by jefft on 11/18/2015.
 */
public interface ReferenceMapStorage extends Actor
{
    Task<Void> addPlayerToMap(SomePlayer player);
    Task<Map<Integer, SomePlayer>> getPlayers();
}
