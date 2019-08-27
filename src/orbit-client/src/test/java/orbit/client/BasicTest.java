/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client;

import kotlinx.coroutines.future.FutureKt;
import orbit.server.OrbitServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class BasicTest {

    @Test
    void basicConnectionTest() {
        final OrbitServer server = new OrbitServer();
        FutureKt.asCompletableFuture(server.start()).join();

        final OrbitClientConfig config = new OrbitClientConfig();
        final OrbitClient client = new OrbitClient(config);
        client.start();

        final String greeting = client.tempGreeter("Joe");
        Assertions.assertThat(greeting).isEqualTo("Hello Joe");

        client.stop();

        FutureKt.asCompletableFuture(server.stop()).join();
    }
}
