package com.ea.orbit.actors.redis.test;

import com.ea.orbit.actors.test.IStorageTestState;

public class HelloState implements IStorageTestState
{

    public String lastName;

    @Override
    public String lastName()
    {
        return lastName;
    }

}
