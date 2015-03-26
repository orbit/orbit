package com.ea.orbit.actors.test.actors;


import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

import java.util.List;

public interface ISomeMatch extends IActor
{
    Task<Void> addPlayer(ISomePlayer player);

    Task<List<ISomePlayer>> getPlayers();

    Task<String> getNodeId();
}
