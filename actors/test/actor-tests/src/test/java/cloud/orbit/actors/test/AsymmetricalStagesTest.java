/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.Execution;
import cloud.orbit.actors.runtime.LazyActorClassFinder;
import cloud.orbit.actors.runtime.Messaging;
import cloud.orbit.actors.test.actors.SomeActor;
import cloud.orbit.actors.test.actors.SomeActorImpl;
import cloud.orbit.actors.test.actors.SomeMatch;
import cloud.orbit.actors.test.actors.SomeMatchActor;
import cloud.orbit.actors.test.actors.SomePlayer;
import cloud.orbit.actors.test.actors.SomePlayerActor;
import cloud.orbit.concurrent.Task;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test nodes that do not contain the same collection of actors.
 */
@SuppressWarnings("unused")
public class AsymmetricalStagesTest extends ActorBaseTest
{


    private List<Class<?>> excludedClasses;

    @Test
    public void asymmetricalNodeTest() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException
    {
        // Asymmetrical nodes, one has Match other has Player, both have SomeActor
        Stage stage1 = createStage(SomeMatchActor.class);
        Stage stage2 = createStage(SomePlayerActor.class);

        final List<Task<String>> tasksA = new ArrayList<>();
        final List<Task<String>> tasksP = new ArrayList<>();
        final List<Task<String>> tasksM = new ArrayList<>();

        stage1.bind();
        Actor.getReference(SomeMatch.class, "100_000").getNodeId().join();
        stage2.bind();
        Actor.getReference(SomePlayer.class, "100_000").getNodeId().join();

        // touching up to 50 different actors of each type.
        for (int i = 0; i < 25; i++)
        {
            stage1.bind();
            tasksA.add(Actor.getReference(SomeActor.class, "100_" + i).getNodeId());
            tasksM.add(Actor.getReference(SomeMatch.class, "100_" + i).getNodeId());
            tasksP.add(Actor.getReference(SomePlayer.class, "200_" + i).getNodeId());

            stage2.bind();
            tasksA.add(Actor.getReference(SomeActor.class, "100_" + i).getNodeId());
            tasksM.add(Actor.getReference(SomeMatch.class, "300_" + i).getNodeId());
            tasksP.add(Actor.getReference(SomePlayer.class, "400_" + i).getNodeId());
        }
        final Set<String> setA = tasksA.stream().map(x -> x.join()).collect(Collectors.toSet());
        final Set<String> setM = tasksM.stream().map(x -> x.join()).collect(Collectors.toSet());
        final Set<String> setP = tasksP.stream().map(x -> x.join()).collect(Collectors.toSet());

        // the SomeActors will be on all nodes
        assertEquals(2, setA.size());
        // all matches must be at the same node
        assertEquals(1, setM.size());
        // all players must be at the same node
        assertEquals(1, setP.size());
        // and it's not the same node
        assertNotEquals(setM, setP);
    }


    private void setField(Object target, String name, Object value) throws IllegalAccessException, NoSuchFieldException
    {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Override
    protected void installExtensions(final Stage stage)
    {
        super.installExtensions(stage);
        final List<Class<?>> classes = this.excludedClasses;
        if (classes != null)
        {
            this.excludedClasses = null;
            stage.addExtension(new LazyActorClassFinder()
            {
                @Override
                public <T extends Actor> Class<? extends T> findActorImplementation(Class<T> actorInterface)
                {
                    Class<? extends T> c = super.findActorImplementation(actorInterface);
                    return classes.contains(c) ? null : c;
                }
            });
        }
    }

    public Stage createStage(Class<?>... excludedActorClasses) throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException
    {
        this.excludedClasses = Arrays.asList(excludedActorClasses);
        return createStage();
    }

    @Test
    public void waitForNodeTest() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException
    {
        loggerExtension.enableDebugFor(Messaging.class);
        loggerExtension.enableDebugFor(Execution.class);
        // Asymmetrical nodes, one has Match other has Player, both have SomeActor
        Stage stage1 = createStage(SomeActorImpl.class, SomePlayerActor.class);

        final Task<String> test = Actor.getReference(SomeMatch.class, "100").getNodeId();

        Stage stage2 = createStage(SomeActorImpl.class, SomeMatchActor.class);

        assertNotNull(test.join());
        dumpMessages();
    }

}
