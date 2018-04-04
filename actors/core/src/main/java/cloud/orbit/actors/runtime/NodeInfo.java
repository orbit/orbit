package cloud.orbit.actors.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.NodeCapabilities.NodeState;

public class NodeInfo
{
    boolean active;
    final NodeAddress address;
    final AtomicBoolean placementGroupPending = new AtomicBoolean(true);
    String placementGroup;
    NodeState state = NodeState.RUNNING;
    NodeCapabilities nodeCapabilities;
    boolean cannotHostActors;
    final ConcurrentHashMap<String, Integer> canActivate = new ConcurrentHashMap<>();
    final Set<String> canActivatePending = ConcurrentHashMap.newKeySet();

    public NodeInfo(final NodeAddress address)
    {
        this.address = address;
    }

    public boolean getActive()
    {
        return this.active;
    }

    public NodeAddress getAddress()
    {
        return this.address;
    }
}
