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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class ExceptionalTest extends ActorBaseTest
{
    public interface ExceptionalThing extends Actor
    {
        Task<String> justRespond();

        Task<String> justThrowAnException();
    }

    @SuppressWarnings("rawtypes")
    public static class ExceptionalThingActor extends AbstractActor implements ExceptionalThing
    {
        public Task<String> justRespond()
        {
            return Task.fromValue("resp");
        }

        public Task<String> justThrowAnException()
        {
            throw new RuntimeException("as requested, one exception!");
        }
    }

    public static class NonSerializableThing
    {

    }

    @Test
    public void noException() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        final ExceptionalThing ref = Actor.getReference(ExceptionalThing.class, "0");
        assertEquals("resp", ref.justRespond().join());
    }

    @Test(expected = CompletionException.class)
    public void withException() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        final ExceptionalThing ref = Actor.getReference(ExceptionalThing.class, "0");
        ref.justThrowAnException().join();
    }

    @Test
    public void catchingTheException() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        final ExceptionalThing ref = Actor.getReference(ExceptionalThing.class, "0");
        try
        {
            ref.justThrowAnException().join();
            fail("should have thrown an exception");
        }
        catch (CompletionException ex)
        {
            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("as requested, one exception!", ex.getCause().getMessage());
        }
    }

    @Test
    public void checkingTheException() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        final ExceptionalThing ref = Actor.getReference(ExceptionalThing.class, "0");
        final Task<String> fut = ref.justThrowAnException();

        final Throwable ex = fut.handle((r, e) -> e).join();

        // The response here sometimes is CompletionException sometimes RuntimeException.
        // It's a jdk bug.
        // https://bugs.openjdk.java.net/browse/JDK-8068432

        assertTrue(fut.isCompletedExceptionally());
        // TODO: as the fixed jdk version (u60) becomes current, test this properly.
        //assertEquals(RuntimeException.class, ex.getClass());
        assertTrue(ex instanceof RuntimeException);
        assertTrue(ex.getMessage(), ex.getMessage().endsWith("as requested, one exception!"));
    }

    //@Test
    // fixed on jdk 8_60
    public void checkCompletableFutureBehaviour()
    {
        // https://bugs.openjdk.java.net/browse/JDK-8068432
        // "Inconsistent exception handling in CompletableFuture.thenCompose"
        // this test will fail once this bug is fixed
        Exception ex = new RuntimeException("x");
        // The order of events change how thenCompose wraps the returned exception
        {
            CompletableFuture<Object> base = new CompletableFuture<>();

            CompletableFuture<String> job = new CompletableFuture<>();
            CompletableFuture<String> composed = base.thenCompose(x -> job);
            job.completeExceptionally(ex);
            base.complete(null);

            Throwable rex = composed.handle((r, e) -> e).join();
            assertNotSame(ex, rex);
            assertEquals(CompletionException.class, rex.getClass());
        }
        // using then compose means the exception gets wrapped
        {
            CompletableFuture<Object> base = new CompletableFuture<>();

            base.complete(null);
            CompletableFuture<String> job = new CompletableFuture<>();
            CompletableFuture<String> composed = base.thenCompose(x -> job);
            job.completeExceptionally(ex);

            Throwable rex = composed.handle((r, e) -> e).join();
            //assertNotSame(ex, rex);
            //assertEquals(CompletionException.class, rex.getClass());
            assertSame(ex, rex);
            assertEquals("Use jdk 1.8.0_60 or above", RuntimeException.class, rex.getClass());
        }
    }
}
