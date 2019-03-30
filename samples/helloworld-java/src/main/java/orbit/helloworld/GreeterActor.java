/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld;

import cloud.orbit.common.logging.Logger;
import cloud.orbit.common.logging.Logging;
import cloud.orbit.core.actor.AbstractActor;

import java.util.concurrent.CompletableFuture;

public class GreeterActor extends AbstractActor implements Greeter  {
    private static Logger logger = Logging.getLogger(GreeterActor.class);

    @Override
    public CompletableFuture<String> greet(String name) {
        logger.info("I was called by: " + name + ". My identity is " + getContext().getReference());
        return CompletableFuture.completedFuture("Hello " + name + "!");
    }
}
