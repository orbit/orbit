/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.leasing;

import orbit.client.util.Concurrent;
import orbit.client.util.ListenableFutureAdapter;
import orbit.shared.proto.NodeManagementGrpc;
import orbit.shared.proto.NodeManagementOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class LeaseManager {
    private static Logger logger = LoggerFactory.getLogger(LeaseManager.class);

    private final NodeManagementGrpc.NodeManagementFutureStub nodeManagement;

    private NodeLease localLease = null;

    public LeaseManager(NodeManagementGrpc.NodeManagementFutureStub nodeManagement) {
        this.nodeManagement = nodeManagement;
    }

    public CompletableFuture start() {
        return ListenableFutureAdapter.toCompletable(
                nodeManagement.joinCluster(
                        NodeManagementOuterClass.JoinClusterRequest.newBuilder().build()
                )
        ).thenAcceptAsync((nodeLease) -> {
            localLease = new NodeLease();
            localLease.setNodeId(nodeLease.getNodeIdentity());
            localLease.setChallenge(nodeLease.getChallengeToken());
            localLease.setExpiresAt(Instant.ofEpochSecond(nodeLease.getExpiresAt().getSeconds()));
            localLease.setRenewAt(Instant.ofEpochSecond((nodeLease.getRenewAt().getSeconds())));
            logger.info("Joined Orbit cluster as node \"" + nodeLease.getNodeIdentity() + "\".");
        }, Concurrent.orbitExecutor);
    }

    public CompletableFuture onTick() {
        if (localLease != null) {
            if (localLease.getRenewAt().isAfter(Instant.now())) {
                return ListenableFutureAdapter.toCompletable(
                        nodeManagement.renewLease(
                                NodeManagementOuterClass.RenewLeaseRequest.newBuilder()
                                        .setNodeIdentity(localLease.getNodeId())
                                        .setChallengeToken(localLease.getChallenge())
                                        .build()
                        )
                ).thenAcceptAsync((leaseRenewal) -> {
                    if(leaseRenewal.getLeaseRenewed()) {
                        localLease.setChallenge(leaseRenewal.getLeaseInfo().getChallengeToken());
                        localLease.setExpiresAt(Instant.ofEpochSecond(leaseRenewal.getLeaseInfo().getExpiresAt().getSeconds()));
                        localLease.setRenewAt(Instant.ofEpochSecond((leaseRenewal.getLeaseInfo().getRenewAt().getSeconds())));
                        logger.debug("Lease renewed. Renew at: " + localLease.getRenewAt().toString() + ". Expires at: " + localLease.getExpiresAt().toString());
                    } else {
                        logger.error("Lease renewal failed.");
                    }
                }, Concurrent.orbitExecutor);
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
