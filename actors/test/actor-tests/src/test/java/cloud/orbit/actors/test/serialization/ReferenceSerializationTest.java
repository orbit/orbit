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

package cloud.orbit.actors.test.serialization;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cluster.NodeAddressImpl;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.RemoteKey;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeGroup;
import cloud.orbit.actors.test.actors.Hello;
import cloud.orbit.actors.test.actors.SomeMatch;
import cloud.orbit.actors.test.actors.SomePlayer;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.ea.async.Async.await;
import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class ReferenceSerializationTest extends ActorBaseTest
{

    public interface MapSer extends Actor
    {
        Task test(String key, Hello reference);
    }

    public static class MapSerActor extends AbstractActor<MapSerActor.State> implements MapSer
    {
        public static class State
        {
            Map<String, Hello> map = new HashMap<>();
        }

        public Task test(String key, Hello reference)
        {
            state().map.put(key, reference);
            await(writeState());
            return readState();
        }
    }

    public interface MiscSer extends Actor
    {
        Task<MiscActor.State> test(String key, Hello reference);

        Task<MiscActor.State> testThis(String key);
    }

    public static class MiscActor extends AbstractActor<MiscActor.State> implements MiscSer
    {
        public static class State implements Serializable
        {
            Hello reference;
            Map<String, Hello> map = new HashMap<>();
            Set<Hello> set = new HashSet<>();
            List<Hello> list = new ArrayList<>();


            MiscSer reference2;
            Map<String, MiscSer> map2 = new HashMap<>();
            Set<MiscSer> set2 = new HashSet<>();
            List<MiscSer> list2 = new ArrayList<>();
        }

        public Task<MiscActor.State> test(String key, Hello reference)
        {
            state().map.put(key, reference);
            state().list.add(reference);
            state().set.add(reference);
            state().reference = reference;
            await(writeState());
            await(readState());
            return Task.fromValue(state());

        }

        public Task<MiscActor.State> testThis(String key)
        {
            final MiscSer reference = this;
            state().map2.put(key, reference);
            state().list2.add(reference);
            state().set2.add(reference);
            state().reference2 = reference;
            await(writeState());
            await(readState());
            return Task.fromValue(state());
        }
    }


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

        Set<Class<?>> validClasses = new HashSet<>(Arrays.asList(RemoteKey.class, NodeAddressImpl.class, String.class));

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

    @Test
    public void mapSerializationTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        MapSer mapSer = Actor.getReference(MapSer.class, "300");

        mapSer.test("blah", Actor.getReference(Hello.class, "11")).join();
    }


    @Test
    public void miscSerializationTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        MiscSer mapSer = Actor.getReference(MiscSer.class, "300");
        final MiscActor.State res
                = mapSer.test("blah", Actor.getReference(Hello.class, "11")).join();
        assertEquals(1, res.list.size());
        assertEquals(1, res.map.size());
        assertNotNull(res.reference);
    }

    @Test
    public void thisSerializationTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        MiscSer mapSer = Actor.getReference(MiscSer.class, "300");
        final MiscActor.State res
                = mapSer.testThis("blah").join();
        assertEquals(1, res.list2.size());
        assertEquals(1, res.map2.size());
        assertNotNull(res.reference2);
    }


}
