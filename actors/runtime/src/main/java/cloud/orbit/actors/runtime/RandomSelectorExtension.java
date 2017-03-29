package cloud.orbit.actors.runtime;

import java.util.List;
import java.util.Random;

import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.extensions.NodeSelectorExtension;

public class RandomSelectorExtension implements NodeSelectorExtension
{

    private final Random random = new Random();

    @Override
    public NodeInfo select(final String interfaceClassName, final NodeAddress localAddress, final List<NodeInfo> potentialNodes)
    {
      return potentialNodes.get(random.nextInt(potentialNodes.size()));
    }

}
