/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import orbit.client.connection.ConnectionManager;
import orbit.shared.proto.ConnectionGrpc;

public final class OrbitClient {
    private final OrbitClientConfig config;
    private ManagedChannel channel = null;
    private ConnectionManager connectionHandler = null;

    public OrbitClient(final OrbitClientConfig config) {
        this.config = config;
    }

    public void start() {
        channel = ManagedChannelBuilder
                .forAddress(config.getGrpcHost(), config.getGrpcPort())
                .usePlaintext()
                .build();

        connectionHandler = new ConnectionManager(ConnectionGrpc.newStub(channel));
        getConnectionHandler().connect();
    }

    public void stop() {
        channel.shutdown();
        getConnectionHandler().disconnect();
        connectionHandler = null;
    }

    public ConnectionManager getConnectionHandler() {
        return connectionHandler;
    }
}
