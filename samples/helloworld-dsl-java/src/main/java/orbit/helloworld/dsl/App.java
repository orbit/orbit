/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld.dsl;

import cloud.orbit.common.logging.Logger;
import cloud.orbit.core.actor.AbstractActor;
import cloud.orbit.runtime.stage.Stage;
import orbit.helloworld.dsl.data.Greeting;

import java.util.concurrent.CompletableFuture;

import static cloud.orbit.common.logging.Logging.getLogger;

public class App {
    public static void main(String[] args) {
        Logger logger = getLogger("app");
        Stage stage = new Stage();

        stage.start().thenCompose(ignored -> {
            Greeter greeter = stage.getActorProxyFactory().getReference(Greeter.class, "test");
            return greeter.greet("Cesar").thenCompose(greeting -> {
                logger.info(greeting.getGreeting());
                return stage.stop();
            });
        }).join();
    }

    public static class GreeterActor extends AbstractActor implements Greeter {
        @Override
        public CompletableFuture<Greeting> greet(String name) {
            return CompletableFuture.completedFuture(new Greeting("Hello " + name + "!"));
        }
    }
}
