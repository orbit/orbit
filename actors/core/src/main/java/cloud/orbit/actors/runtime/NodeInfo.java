package cloud.orbit.actors.runtime;

import java.util.concurrent.ConcurrentHashMap;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.NodeCapabilities.NodeState;

public class NodeInfo
{
    boolean active;
    final NodeAddress address;
    NodeState state = NodeState.RUNNING;
    NodeCapabilities nodeCapabilities;
    boolean cannotHostActors;
    final ConcurrentHashMap<String, Integer> canActivate = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Boolean> canActivatePending = new ConcurrentHashMap<>();

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
