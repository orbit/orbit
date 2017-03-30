package cloud.orbit.actors.extensions;

import java.util.List;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.actors.runtime.NodeInfo;

public interface NodeSelectorExtension extends ActorExtension
{

    NodeInfo select(String interfaceClassName, NodeAddress localAddress, List<NodeInfo> potentialNodes);

}
