/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import orbit.shared.proto.GreeterGrpc;
import orbit.shared.proto.GreeterOuterClass;

public final class OrbitClient {
    private final OrbitClientConfig config;
    private ManagedChannel channel = null;

    public OrbitClient(final OrbitClientConfig config) {
        this.config = config;
    }

    public void start() {
        channel = ManagedChannelBuilder
                .forAddress(config.getGrpcHost(), config.getGrpcPort())
                .usePlaintext()
                .build();
    }

    public void stop() {
        if(channel != null) channel.shutdown();
    }

    public String tempGreeter(final String name) {
        GreeterGrpc.GreeterBlockingStub greeter = GreeterGrpc.newBlockingStub(channel);
        GreeterOuterClass.HelloRequest request = GreeterOuterClass.HelloRequest.newBuilder().setName(name).build();
        GreeterOuterClass.HelloReply response;
        response = greeter.sayHello(request);
        return response.getMessage();
    }

}
