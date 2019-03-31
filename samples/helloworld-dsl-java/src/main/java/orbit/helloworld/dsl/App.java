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
import orbit.helloworld.dsl.data.Language;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cloud.orbit.common.logging.Logging.getLogger;

public class App {
    public static void main(String[] args) {
        Logger logger = getLogger("app");
        Stage stage = new Stage();

        stage.start().thenCompose(ignored -> {
            Greeter greeter = stage.getActorProxyFactory().createProxy(Greeter.class, "test");
            return greeter.greet("Cesar").thenCompose(greetings -> {
                greetings.forEach((language, greeting) ->
                        logger.info("In {}: {}", greeting.getLanguage(), greeting.getText()));
                return stage.stop();
            });
        }).join();
    }

    public static class GreeterActor extends AbstractActor implements Greeter {
        @Override
        public CompletableFuture<Map<Language, Greeting>> greet(String name) {
            return CompletableFuture.completedFuture(
                    Stream.of(
                            new Greeting(Language.ENGLISH, "Hello, " + name + "!"),
                            new Greeting(Language.GERMAN, "Hello, " + name + "!"))
                            .collect(Collectors.toMap(Greeting::getLanguage, Function.identity())));
        }
    }
}
