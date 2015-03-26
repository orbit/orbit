package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

public interface ISomePlayer extends IActor
{
    Task<String> getName();

    Task<Void> joinMatch(ISomeMatch someMatch);

    Task<Void> matchEvent(ISomeMatch someMatch);

    Task<Integer> getMatchEventCount();

    Task<String> getNodeId();
}
