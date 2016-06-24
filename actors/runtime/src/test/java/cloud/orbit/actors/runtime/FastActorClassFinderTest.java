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

package cloud.orbit.actors.runtime;

import org.junit.Before;
import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Remindable;
import cloud.orbit.actors.annotation.NoIdentity;
import cloud.orbit.actors.annotation.OneWay;
import cloud.orbit.concurrent.Task;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public class FastActorClassFinderTest
{
    private FastActorClassFinder actorClassFinder;

    @Before
    public void setUp() throws Exception
    {
        actorClassFinder = new FastActorClassFinder("cloud.orbit");
        actorClassFinder.start().join();
    }

    @Test
    public void testFindActorInterfaces() throws Exception
    {
        Collection<Class<? extends Actor>> actorInterfaces = actorClassFinder.findActorInterfaces(ScheduledTask.class::isAssignableFrom);
        assertEquals(2, actorInterfaces.size());
    }

    public class Test1ScheduledTaskActor extends AbstractScheduledTaskActor implements Test1ScheduledTask
    {

    }

    public class Test2ScheduledTaskActor extends AbstractScheduledTaskActor implements Test2ScheduledTask
    {

    }

    @NoIdentity
    public interface Test1ScheduledTask extends ScheduledTask
    {

    }

    @NoIdentity
    public interface Test2ScheduledTask extends ScheduledTask
    {

    }

    public abstract class AbstractScheduledTaskActor<T> extends AbstractActor<T> implements ScheduledTask
    {
        @Override
        public Task<Void> ensureStart()
        {
            return Task.done();
        }

        @Override
        public Task<?> receiveReminder(final String reminderName, final TickStatus status)
        {
            return Task.done();
        }
    }

    public interface ScheduledTask extends Remindable
    {
        @OneWay
        Task<Void> ensureStart();
    }
}
