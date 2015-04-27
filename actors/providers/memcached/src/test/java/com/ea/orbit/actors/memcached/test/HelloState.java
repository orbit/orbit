package com.ea.orbit.actors.memcached.test;

import com.ea.orbit.actors.test.IStorageTestState;

import java.io.Serializable;

public class HelloState implements IStorageTestState, Serializable
{

    public String lastName;

    @Override
    public String lastName()
    {
        return lastName;
    }

}
