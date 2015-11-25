package com.ea.orbit.actors.peer.streams;


import com.ea.orbit.actors.ClientObject;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.concurrent.Task;

public interface ClientSideStreamProxy extends ClientObject
{
    @OneWay
    Task<Void> onNext(String provider, String streamId, Object message);

}
