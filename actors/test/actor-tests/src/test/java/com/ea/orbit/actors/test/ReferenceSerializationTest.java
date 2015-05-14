/*
Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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

package com.ea.orbit.actors.test;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.cluster.NodeAddressImpl;
import com.ea.orbit.actors.runtime.ActorKey;
import com.ea.orbit.actors.test.actors.SomeMatch;
import com.ea.orbit.actors.test.actors.SomePlayer;

import org.junit.Test;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.fail;

@SuppressWarnings("unused")
public class ReferenceSerializationTest extends ActorBaseTest
{
    @Test
    public void referencePassingTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
        SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
        someMatch.addPlayer(somePlayer).join();
    }


    @Test
    public void passingActorInsteadOfReferenceTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
        SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
        somePlayer.joinMatch(someMatch).join();
    }

    /**
     * Asserts that no classes other than NodeAddress and ActorKey get into the distributed caches
     */
    @Test
    public void distributedDirectoryClassesTest() throws ExecutionException, InterruptedException
    {

        Stage stage1 = createStage();
        Stage client = createClient();
        client.bind();
        SomeMatch someMatch = Actor.getReference(SomeMatch.class, "300");
        SomePlayer somePlayer = Actor.getReference(SomePlayer.class, "101");
        somePlayer.joinMatch(someMatch).join();


        Set<Class<?>> validClasses = Sets.newHashSet(ActorKey.class, NodeAddressImpl.class, String.class);

        for (Map.Entry e : FakeGroup.get(clusterName).getCaches().entrySet())
        {
            Map<Object, Object> cache = (Map) e.getValue();
            for (Map.Entry e2 : cache.entrySet())
            {
                Object key = e2.getKey();
                if (!validClasses.contains(key.getClass()))
                {
                    fail("Invalid class found in the distributed cache: " + key.getClass());
                }

                Object value = e2.getValue();
                if (!validClasses.contains(value.getClass()))
                {
                    fail("Invalid class found in the distributed cache: " + value.getClass());
                }
            }
        }
    }


}
