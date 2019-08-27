/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client;

public class OrbitClientConfig {
    private String grpcHost = "localhost";
    private Integer grpcPort = 50056;

    public String getGrpcHost() {
        return grpcHost;
    }

    public void setGrpcHost(String grpcHost) {
        this.grpcHost = grpcHost;
    }

    public Integer getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(Integer grpcPort) {
        this.grpcPort = grpcPort;
    }
}
