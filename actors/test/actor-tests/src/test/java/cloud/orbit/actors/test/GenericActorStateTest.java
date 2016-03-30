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


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class GenericActorStateTest extends ActorBaseTest
{
    public interface GenActor extends Actor
    {
        Task<Point> getPoint();

        Task<Void> setPoint(Point point);
    }

    public static class ParametrizedState<T>
    {
        private T data;
    }

    public static class Point implements Serializable
    {
        int x, y;
    }

    public static class MyActorImpl extends AbstractActor<ParametrizedState<Point>> implements GenActor
    {
        public Task<Point> getPoint()
        {
            return Task.fromValue(state().data);
        }

        public Task<Void> setPoint(Point point)
        {
            state().data = point;
            return writeState();
        }
    }

    /**
     * Ensures that is possible save and restore the state with parametrized states.
     */
    @Test
    public void testGenericStateActor() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        assertEquals(0, fakeDatabase.values().size());
        final Point p1 = new Point();
        p1.x = 1;
        p1.y = 2;
        Actor.getReference(GenActor.class, "300").setPoint(p1).join();

        stage1.stop().join();

        Stage stage2 = createStage();
        Point p2 = Actor.getReference(GenActor.class, "300").getPoint().join();
        assertEquals(1, p2.x);
        assertEquals(2, p2.y);

        assertEquals("{\"data\":{\"x\":1,\"y\":2}}", fakeDatabase.values().stream().findFirst().get().toString());

    }
}
