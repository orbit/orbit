package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.junit.Test;

public class ActivationFailureTest extends ActorBaseTest
{

    public interface Hello extends Actor
    {
        Task<String> sayHello();
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<String> sayHello()
        {
            return Task.fromValue("hello");
        }

        @Override
        public Task<?> activateAsync()
        {
            throw new UncheckedException("Intentional failure");
        }

    }

    @Test(timeout = 30_000L)
    public void test()
    {
        createStage();
        expectException(() -> Actor.getReference(Hello.class, "0").sayHello().join());
        // second time also since the actor can't have been activated.
        expectException(() -> Actor.getReference(Hello.class, "0").sayHello().join());
        dumpMessages();
    }
}
