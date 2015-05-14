package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;


@StatelessWorker
public interface StatelessThing extends IActor
{
    Task<UUID> getUniqueActivationId();

    Task<UUID> getUniqueActivationId(long sleepNanos);
}
