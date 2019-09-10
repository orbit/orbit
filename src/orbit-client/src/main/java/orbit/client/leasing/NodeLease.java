/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.leasing;

import java.time.Instant;
import java.util.Date;

public class NodeLease {
    private String nodeId;
    private String challenge;
    private Instant expiresAt;
    private Instant renewAt;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRenewAt() {
        return renewAt;
    }

    public void setRenewAt(Instant renewAt) {
        this.renewAt = renewAt;
    }
}
