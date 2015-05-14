package com.ea.orbit.actors.test.actors;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.Task;

import java.util.List;

public interface SomeMatch extends Actor
{
    Task<Void> addPlayer(SomePlayer player);

    Task<List<SomePlayer>> getPlayers();

    Task<String> getNodeId();
}
