package com.ea.orbit.actors.test.actors;


import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

import java.util.List;

public interface SomeMatch extends IActor
{
    Task<Void> addPlayer(SomePlayer player);

    Task<List<SomePlayer>> getPlayers();

    Task<String> getNodeId();
}
