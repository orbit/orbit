/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.connection;

import io.grpc.stub.StreamObserver;
import orbit.shared.proto.Messages;

public class ConnectionObserver implements StreamObserver<Messages.Message> {
    @Override
    public void onNext(Messages.Message message) {
        System.out.println("HELLO");
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }
}
