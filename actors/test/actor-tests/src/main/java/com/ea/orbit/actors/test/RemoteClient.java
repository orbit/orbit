package com.ea.orbit.actors.test;

import com.ea.orbit.actors.runtime.BasicRuntime;

public interface RemoteClient extends BasicRuntime
{
    void cleanup(boolean block);

}
