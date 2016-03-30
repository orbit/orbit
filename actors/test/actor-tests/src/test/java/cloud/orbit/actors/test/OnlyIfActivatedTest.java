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
import cloud.orbit.actors.test.actors.OnlyIfActivated;
import cloud.orbit.actors.test.actors.OnlyIfActivatedActor;
import cloud.orbit.exception.UncheckedException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OnlyIfActivatedTest extends ActorBaseTest
{
    private Stage stage;

    @Test
    public void onlyIfActivatedTest()
    {
        OnlyIfActivated only = Actor.getReference(OnlyIfActivated.class, "234");
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        Assert.assertEquals(0, OnlyIfActivatedActor.accessCount);
        only.makeActiveNow().join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        only.doSomethingSpecial("A").join();
        assertEquals(5, OnlyIfActivatedActor.accessCount);
    }

    @Before
    public void initializeStage()
    {
        try
        {
            stage = createStage();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }
}
