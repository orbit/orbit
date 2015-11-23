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

package com.ea.orbit.actors.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.ea.orbit.actors.transactions.TransactionUtils.transaction;
import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class DisjunctTransactionTest extends ActorBaseTest
{
    public interface Disjunct extends Actor
    {
        Task<Integer> local(int i1, int i2);

        Task<Integer> getBalance();

        Task<Integer> singleLevel(int i1);

        Task<Integer> remote(int i1, Disjunct other, int i2);
    }

    public static class DisjunctActor extends EventSourcedActor<DisjunctActor.State> implements Disjunct
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionalEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<Integer> local(int i1, int i2)
        {
            // Start Transaction
            await(transaction(() ->
            {
                state().incrementBalance(i1);
                if (i1 < 0)
                {
                    throw new IllegalArgumentException("i1: " + i1);
                }
                return Task.done();
            }));
            await(transaction(() ->
            {
                state().incrementBalance(i2);
                if (i2 < 0)
                {
                    throw new IllegalArgumentException("i2: " + i2);
                }
                return Task.done();
            }));
            return Task.fromValue(state().balance);
            // End Transaction
        }

        @Override
        public Task<Integer> remote(int i1, Disjunct other, int i2)
        {

            // Start Transaction
            await(transaction(() ->
            {
                state().incrementBalance(i1);
                if (i1 < 0)
                {
                    // the nested transaction was completed
                    // this will force the parent to cancel the nested
                    throw new IllegalArgumentException("i1: " + i1);
                }
                return Task.done();
            }));
            final int r1 = await(other.singleLevel(i2));
            return Task.fromValue(state().balance + r1);
            // End Transaction
        }

        @Override
        public Task<Integer> singleLevel(int i)
        {
            return transaction(() ->
            {
                state().incrementBalance(i);
                if (i < 0)
                {
                    throw new IllegalArgumentException("i: " + i);
                }
                return Task.fromValue(state().balance);
            });
        }


        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }

        @Override
        public Task<?> activateAsync()
        {
            getLogger().info("activate");
            return super.activateAsync();
        }
    }

    @Test
    public void disjuntSuccessful() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        assertEquals(3, (int) jimmy.local(1, 2).join());
    }

    @Test(timeout = 10_000L)
    public void localFailureInTheSecond() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        assertEquals(6, (int) jimmy.local(1, 5).join());

        assertEquals("i2: -20", expectException(() -> jimmy.local(10, -20).join()).getCause().getMessage());

        // the first transaction is not cancelled
        eventually(() -> assertEquals(16, (int) jimmy.getBalance().join()));
    }

    @Test(timeout = 10_000L)
    public void localFailureInTheFirst() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        assertEquals(3, (int) jimmy.local(1, 2).join());

        assertEquals("i1: -1", expectException(() -> jimmy.local(-1, 2).join()).getCause().getMessage());
        // the second transaction never happens
        eventually(() -> assertEquals(3, (int) jimmy.getBalance().join()));
    }

    @Test
    public void remote() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        Disjunct other = Actor.getReference(Disjunct.class, "other");
        assertEquals(3, (int) jimmy.remote(1, other, 2).join());
        assertEquals(1, (int) jimmy.getBalance().join());
        assertEquals(2, (int) other.getBalance().join());
    }

    @Test
    public void remoteFailureInTheSecond() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        Disjunct other = Actor.getReference(Disjunct.class, "other");
        assertEquals(3, (int) jimmy.remote(1, other, 2).join());
        assertEquals(1, (int) jimmy.getBalance().join());
        assertEquals(2, (int) other.getBalance().join());

        assertEquals("i: -2", expectException(() -> jimmy.remote(1, other, -2).join()).getCause().getMessage());

        // the first transaction is not canceled
        eventually(() -> assertEquals(2, (int) jimmy.getBalance().join()));
        // the second transaction was reverted
        eventually(() -> assertEquals(2, (int) other.getBalance().join()));
    }

    @Test
    public void remoteFailureInTheFirst() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Disjunct jimmy = Actor.getReference(Disjunct.class, "1");
        Disjunct other = Actor.getReference(Disjunct.class, "other");
        // a successful transaction
        assertEquals(3, (int) jimmy.remote(1, other, 2).join());

        // the failure
        assertEquals("i1: -1", expectException(() -> jimmy.remote(-1, other, 2).join()).getCause().getMessage());

        eventually(() -> assertEquals(1, (int) jimmy.getBalance().join()));
        assertEquals(2, (int) other.getBalance().join());
        dumpMessages();
    }

}

