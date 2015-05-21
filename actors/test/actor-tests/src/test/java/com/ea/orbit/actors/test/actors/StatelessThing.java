package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.annotation.StatelessWorker;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;


@StatelessWorker
public interface StatelessThing extends Actor
{
    Task<UUID> getUniqueActivationId();

    Task<UUID> getUniqueActivationId(long sleepNanos);
}
