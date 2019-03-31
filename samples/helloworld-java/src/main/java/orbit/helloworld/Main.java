/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.helloworld;

import cloud.orbit.common.logging.Logger;
import cloud.orbit.common.logging.Logging;
import cloud.orbit.runtime.stage.Stage;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logging.getLogger("main");
        Stage stage = new Stage();
        stage.start().join();
        Greeter greeter = stage.getActorProxyFactory().createProxy(Greeter.class);
        String greeting = greeter.greet("Joe").join();
        logger.info("Response: " + greeting);
        stage.stop();
    }
}
