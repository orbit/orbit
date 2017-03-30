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

import cloud.orbit.concurrent.TaskContext;

public class ActorTaskContext extends TaskContext
{
    private AbstractActor<?> actor;
    private ActorRuntime runtime;

    /**
     * Creates a new actor task context and pushes it to the current thread context stack.
     *
     * @return the new execution context
     */
    public static ActorTaskContext pushNew()
    {
        final ActorTaskContext context = new ActorTaskContext();
        context.push();
        return context;
    }

    void setActor(final AbstractActor<?> actor)
    {
        this.actor = actor;
        if (actor != null)
        {
            this.runtime = actor.runtime;
        }
    }

    public AbstractActor<?> getActor()
    {
        return actor;
    }

    public static ActorTaskContext current()
    {
        final TaskContext current = TaskContext.current();
        if (current instanceof ActorTaskContext)
        {
            return (ActorTaskContext) current;
        }
        return null;
    }

    /**
     * Finds out what task context is active in a certain thread.
     * This method is is available here for debugging and profiling.
     *
     * @return the actor task context associated to the thread
     */
    public static ActorTaskContext currentFor(Thread thread)
    {
        final TaskContext current = TaskContext.currentFor(thread);
        if (current instanceof ActorTaskContext)
        {
            return (ActorTaskContext) current;
        }
        return null;
    }

    void setRuntime(final ActorRuntime runtime)
    {
        this.runtime = runtime;
    }

    public ActorRuntime getRuntime()
    {
        return runtime;
    }

    public ActorTaskContext cloneContext()
    {
        final ActorTaskContext tc = new ActorTaskContext();
        tc.actor = actor;
        tc.runtime = runtime;
        tc.properties().putAll(properties());
        return tc;
    }

    public static AbstractActor<?> currentActor()
    {
        final ActorTaskContext current = current();
        return current != null ? current.getActor() : null;
    }
}
