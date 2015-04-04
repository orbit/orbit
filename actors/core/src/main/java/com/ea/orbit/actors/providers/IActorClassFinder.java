package com.ea.orbit.actors.providers;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.providers.IOrbitProvider;

public interface IActorClassFinder extends IOrbitProvider
{
    <T extends IActor> Class<? extends T> findActorImplementation(Class<T> iActorInterface);
}
