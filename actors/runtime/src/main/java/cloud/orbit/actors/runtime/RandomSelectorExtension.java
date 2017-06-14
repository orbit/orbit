package cloud.orbit.actors.runtime;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.NodeSelectorExtension;

public class RandomSelectorExtension implements NodeSelectorExtension
{

    @Override
    public NodeInfo select(final String interfaceClassName, final NodeAddress localAddress, final List<NodeInfo> potentialNodes)
    {
      return potentialNodes.get(ThreadLocalRandom.current().nextInt(potentialNodes.size()));
    }

}
