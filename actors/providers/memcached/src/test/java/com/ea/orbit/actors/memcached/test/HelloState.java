package com.ea.orbit.actors.memcached.test;

import com.ea.orbit.actors.ObserverManager;
import com.ea.orbit.actors.test.IStorageTestState;

import java.io.Serializable;

public class HelloState implements IStorageTestState, Serializable
{
    ObserverManager<IHelloObserver> observers = new ObserverManager<>();

    public String lastName;

    public ObserverManager<IHelloObserver> getObservers()
    {
        return observers;
    }

    @Override
    public String lastName()
    {
        return lastName;
    }

}
