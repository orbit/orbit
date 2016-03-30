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

import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ReminderController;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;


public class TestLifecycleLog implements LifetimeExtension
{
    private TestLogger logger;
    private String name;

    public TestLifecycleLog(final TestLogger logger, String name)
    {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public Task<?> preActivation(final AbstractActor<?> actor)
    {
        if (actor instanceof ReminderController)
        {
            return Task.done();
        }
        String to = toString(actor);
        logger.sequenceDiagram.add("hnote over \"" + to + "\" #white : [" + name + "] activate");
        ///logger.sequenceDiagram.add("rnote right of \"" + to + "\" #white : [" + name + "] activate");
        //logger.sequenceDiagram.add("\"" + to + "\" --> \"" + to + "\" : [" + name + "] activate");
        //logger.sequenceDiagram.add("]--> \"" + to + "\" : [" + name + "] activate");
        //logger.sequenceDiagram.add("\"" + to + "\" -->o \"" + to + "\" : [" + name + "] activate");
        return Task.done();
    }

    @Override
    public Task<?> postDeactivation(final AbstractActor<?> actor)
    {
        if (actor instanceof ReminderController)
        {
            return Task.done();
        }
        String to = toString(actor);
        logger.sequenceDiagram.add("hnote over \"" + to + "\" #white : [" + name + "] deactivate");
        //logger.sequenceDiagram.add("rnote right of \"" + to + "\" #white : [" + name + "] deactivate");
        //logger.sequenceDiagram.add("destroy \"" + to + "\"");
        //logger.sequenceDiagram.add("\"" + to + "\" -->x \"" + to + "\" : [" + name + "] deactivate");
        return Task.done();
    }

    String toString(Object obj)
    {
        if (obj instanceof String)
        {
            return (String) obj;
        }
        if (obj instanceof AbstractActor)
        {
            final RemoteReference ref = RemoteReference.from((AbstractActor) obj);
            return RemoteReference.getInterfaceClass(ref).getSimpleName() + ":" +
                    RemoteReference.getId(ref);
        }
        if (obj instanceof RemoteReference)
        {
            return RemoteReference.getInterfaceClass((RemoteReference<?>) obj).getSimpleName() + ":" +
                    RemoteReference.getId((RemoteReference<?>) obj);
        }
        return String.valueOf(obj);
    }

}
