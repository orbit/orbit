/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.connection;

import io.grpc.*;
import orbit.client.leasing.LeaseManager;

public class NodeIdInterceptor implements ClientInterceptor {
    final LeaseManager leaseManager;

    static final Metadata.Key<String> NODE_ID =
            Metadata.Key.of("nodeId", Metadata.ASCII_STRING_MARSHALLER);

    public NodeIdInterceptor(final LeaseManager leaseManager) {
        this.leaseManager = leaseManager;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                headers.put(NODE_ID, leaseManager.getLocalLease().getNodeId());
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}