/*
 Copyright (C) 2018 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.test;

import org.junit.Assert;
import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.NodeInfo;
import cloud.orbit.actors.runtime.RandomSelectorExtension;
import cloud.orbit.actors.test.actors.PreferLocalActor;
import cloud.orbit.actors.test.actors.SomeActor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PlacementGroupsTest extends ActorBaseTest
{
    static class RecordingNodeSelectorExtension extends RandomSelectorExtension
    {
        volatile List<NodeInfo> lastPotentialNodes;

        @Override
        public NodeInfo select(final String interfaceClassName, final NodeAddress localAddress, final List<NodeInfo> potentialNodes)
        {
            lastPotentialNodes = potentialNodes;
            return super.select(interfaceClassName, localAddress, potentialNodes);
        }
    }

    @Test
    public void defaultPlacementGroupTest()
    {
        Stage stage = createStage();
        assertEquals("default", stage.getPlacementGroup());
        assertEquals(Collections.singleton("default"), stage.getHosting().getTargetPlacementGroups());
    }

    @Test
    public void initialTargetPlacementGroupsTest()
    {
        Stage stage = createStage(builder -> builder.placementGroup("0"));
        assertEquals("0", stage.getPlacementGroup());
        assertEquals(Collections.singleton("0"), stage.getHosting().getTargetPlacementGroups());
    }

    @Test
    public void singlePlacementGroupTest()
    {
        Stage stage0 = createStage(builder -> builder.placementGroup("0"));
        Stage stage1 = createStage(builder -> builder.placementGroup("0"));

        final long baseCount = stage0.getLocalObjectCount() + stage1.getLocalObjectCount();

        Actor.getReference(SomeActor.class, "1").sayHello("hello").join();

        assertEquals(baseCount + 1, stage0.getLocalObjectCount() + stage1.getLocalObjectCount());
    }

    @Test
    public void multiplePlacementGroupsTest()
    {
        RecordingNodeSelectorExtension nodeSelector = new RecordingNodeSelectorExtension();

        Stage stage0 = createStage(builder -> {
            builder.extensions(nodeSelector);
            builder.placementGroup("0");
        });

        SomeActor actor = Actor.getReference(SomeActor.class, UUID.randomUUID().toString());
        assertEquals(stage0.runtimeIdentity(), actor.getNodeId().join());
        assertEquals(1, nodeSelector.lastPotentialNodes.size());

        Stage stage1 = createStage(builder -> {
            builder.extensions(nodeSelector);
            builder.placementGroup("1");
        });

        // No need to do this on stage0 since stage1 is currently bound
        stage1.getHosting().setTargetPlacementGroups(new HashSet<>(Arrays.asList("0", "1")));

        // Because we've just added a new node, we need to wait until both stages know the other stage
        // can activate SomeActor, since that happens asynchronously
        eventuallyTrue(2500, () -> {
            Actor.getReference(SomeActor.class, UUID.randomUUID().toString()).sayHello("hello").join();

            // Return true when nodes in both target groups have been considered for placement
            return nodeSelector.lastPotentialNodes.size() == 2;
        });

        stage0.bind();
        stage0.getHosting().setTargetPlacementGroups(Collections.singleton("1"));

        // No need to call eventuallyTrue here since filtering by target placement group is done synchronously
        SomeActor otherActor = Actor.getReference(SomeActor.class, UUID.randomUUID().toString());
        assertEquals(stage1.runtimeIdentity(), otherActor.getNodeId().join());
        assertEquals(1, nodeSelector.lastPotentialNodes.size());
    }

    @Test
    public void preferLocalPlacementTest()
    {
        Stage stage0 = createStage(builder -> builder.placementGroup("0"));
        Stage stage1 = createStage(builder -> builder.placementGroup("1"));

        stage1.getHosting().setTargetPlacementGroups(new HashSet<>(Arrays.asList("0", "1")));

        // stage1 is currently bound, so the actor will be placed on that node despite the other being
        // available for placement
        PreferLocalActor actor = Actor.getReference(PreferLocalActor.class, UUID.randomUUID().toString());
        assertEquals(stage1.runtimeIdentity(), actor.getNodeId().join());

        // Set target placement group to "0", which will force placement in the other node despite the actor's
        // preference for local placement
        stage1.getHosting().setTargetPlacementGroups(Collections.singleton("0"));

        PreferLocalActor otherActor = Actor.getReference(PreferLocalActor.class, UUID.randomUUID().toString());
        assertEquals(stage0.runtimeIdentity(), otherActor.getNodeId().join());
    }

    @Test
    public void reactivationOnCurrentPlacementTargetGroupTest() {
        clock.stop();

        Stage stage0 = createStage(builder -> builder.placementGroup("0"));
        Stage stage1 = createStage(builder -> builder.placementGroup("1"));

        SomeActor actor = Actor.getReference(SomeActor.class, "1");
        assertEquals(stage1.runtimeIdentity(), actor.getNodeId().join());

        // Advance the clock so the actor deactivates
        final UUID id = actor.getUniqueActivationId().join();
        clock.incrementTime(20, TimeUnit.MINUTES);
        stage1.cleanup().join();

        stage1.getHosting().setTargetPlacementGroups(Collections.singleton("0"));

        Assert.assertNotEquals(id, actor.getUniqueActivationId().join());
        assertEquals(stage0.runtimeIdentity(), actor.getNodeId().join());
    }
}
