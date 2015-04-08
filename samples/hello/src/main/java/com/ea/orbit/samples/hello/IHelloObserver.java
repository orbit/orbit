package com.ea.orbit.samples.hello;

import com.ea.orbit.actors.IActorObserver;
import com.ea.orbit.concurrent.Task;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public interface IHelloObserver extends IActorObserver {
    Task<Void> receiveHello(String greeting);
}
