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


package cloud.orbit.actors.extensions.test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.DefaultActorConstructionExtension;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ConstructionTest {

    private static final String DEFAULT_ID = UUID.randomUUID().toString();

    private static final String OTHER_ID = UUID.randomUUID().toString();

    @Test
    public void testDefaultConstruction() throws Exception {

        final Stage stage = new Stage.Builder()
                .build();

        try {

            stage.start().join();

            final TestConstruction actor = Actor.getReference(TestConstruction.class, "0");
            assertEquals(DEFAULT_ID, actor.getId().get());

        } finally {
            stage.stop().join();
        }

    }

    @Test
    public void testConstruction() throws Exception {

        final Stage stage = new Stage.Builder()
                .extensions(new TestConstructionExtension())
                .build();

        try {

            stage.start().join();

            final TestConstruction actor = Actor.getReference(TestConstruction.class, "0");
            assertEquals(OTHER_ID, actor.getId().get());

        } finally {
            stage.stop().join();
        }

    }

    public static class TestConstructionExtension extends DefaultActorConstructionExtension
    {

        @Override
        public <T> T newInstance(final Class<T> concreteClass) {
            if (concreteClass.equals(TestConstructionActor.class)) {
                final TestConstructionActor testConstructionActor = new TestConstructionActor();
                testConstructionActor.setId(OTHER_ID);
                //noinspection unchecked
                return (T)testConstructionActor;
            }
            return super.newInstance(concreteClass);
        }
    }

    public interface TestConstruction extends Actor {

        Task<String> getId();

    }

    public static class TestConstructionActor extends AbstractActor implements TestConstruction {

        private String id = DEFAULT_ID;

        public void setId(final String id) {
            this.id = id;
        }

        @Override
        public Task<String> getId() {
            return Task.fromValue(this.id);
        }

    }

}
