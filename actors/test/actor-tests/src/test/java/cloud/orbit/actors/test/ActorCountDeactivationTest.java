/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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
import cloud.orbit.actors.extensions.ActorCountDeactivationExtension;
import cloud.orbit.actors.test.actors.SomeActor;

/**
 * Created by joeh on 2017-05-15.
 */
public class ActorCountDeactivationTest extends ActorBaseTest
{
    private static final Integer maxActorCount = 50;
    private static final Integer targetActorCount = 25;

    @Test
    public void testActorCountDeactivation() {
        clock.stop();
        Stage stage = createStage();

        final Integer actorsToAdd = maxActorCount * 2;
        long baseCount = stage.getLocalObjectCount();

        for(Integer i = 0; i < actorsToAdd; ++i) {
            Actor.getReference(SomeActor.class, i.toString()).sayHello("Hello").join();
        }

        Assert.assertTrue( stage.getLocalObjectCount() >= baseCount + actorsToAdd);

        // Should trigger cleanup even without time passing
        baseCount = stage.getLocalObjectCount();
        stage.cleanup().join();
        stage.cleanup().join();

        Assert.assertTrue(stage.getLocalObjectCount() <= baseCount - targetActorCount);

    }

    @Override
    protected void installExtensions(final Stage stage)
    {
        stage.addExtension(new ActorCountDeactivationExtension(maxActorCount, targetActorCount));
    }
}
