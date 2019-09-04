/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.connection;

import io.grpc.stub.StreamObserver;
import orbit.shared.proto.ConnectionGrpc;
import orbit.shared.proto.Messages;

public class ConnectionManager {
    private ConnectionGrpc.ConnectionStub stub;
    private StreamObserver<Messages.Message> observer;

    public ConnectionManager(ConnectionGrpc.ConnectionStub stub) {
        this.stub = stub;
    }

    public void connect() {
        observer = stub.messages(new ConnectionObserver());
    }

    public void disconnect() {
        observer.onError(new Throwable("Connection end"));
    }

    public void sendMessage(Messages.Message message) {
        observer.onNext(message);
    }
}
