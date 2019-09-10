/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import orbit.client.connection.ConnectionManager;
import orbit.client.leasing.LeaseManager;
import orbit.client.util.Concurrent;
import orbit.shared.proto.ConnectionGrpc;
import orbit.shared.proto.NodeManagementGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public final class OrbitClient {
    private static Logger logger = LoggerFactory.getLogger(OrbitClient.class);

    private final OrbitClientConfig config;
    private ManagedChannel channel = null;
    private ConnectionManager connectionHandler = null;
    private LeaseManager leaseManager = null;

    private Timer timer = null;

    public OrbitClient(final OrbitClientConfig config) {
        this.config = config;
    }

    public CompletableFuture start() {
        return CompletableFuture.runAsync(() -> {
                    channel = ManagedChannelBuilder
                            .forAddress(config.getGrpcHost(), config.getGrpcPort())
                            .usePlaintext()
                            .build();

                    leaseManager = new LeaseManager(
                            NodeManagementGrpc.newFutureStub(channel)
                    );
                    startTimer();
                }, Concurrent.orbitExecutor
        ).thenCompose((unused) -> {
                    return getLeaseManager().start();
                }
        ).thenRun(() -> {
            connectionHandler = new ConnectionManager(ConnectionGrpc.newStub(channel));
            connectionHandler.connect();
        });
    }

    public CompletableFuture stop() {
        return CompletableFuture.runAsync(() -> {
            channel.shutdown();
            getConnectionHandler().disconnect();
            connectionHandler = null;
        });
    }

    private void startTimer() {
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    onTick();
                } catch (Throwable t) {
                    logger.error("Unhandled exception during tick", t);
                }
            }
        };
        timer = new Timer("orbit-timer");
        timer.scheduleAtFixedRate(timerTask, config.getTickRate(), config.getTickRate());
    }

    private void onTick() {
        getLeaseManager().onTick().join();
    }

    public ConnectionManager getConnectionHandler() {
        return connectionHandler;
    }

    public LeaseManager getLeaseManager() {
        return leaseManager;
    }
}
