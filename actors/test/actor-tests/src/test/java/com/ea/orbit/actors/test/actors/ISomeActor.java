package com.ea.orbit.actors.test.actors;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

import java.util.UUID;

public interface ISomeActor extends IActor
{
    Task<String> sayHello(String greeting);

    Task<UUID> getUniqueActivationId();

    Task<UUID> getUniqueActivationId(long sleepNanos);

    Task<Boolean> getActivationWasCalled();

    Task<String> getNodeId();
}
