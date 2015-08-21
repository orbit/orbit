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
public class NestedTransactionTest extends ActorBaseTest
{
    public interface Parent extends Actor
    {
        Task<Integer> localNesting(int i1, int i2);

        Task<Integer> getBalance();

        Task<Integer> singleLevel(int i1);

        Task<Integer> remoteNested(int i1, Parent other, int i2);
    }

    public static class ParentActor extends EventSourcedActor<ParentActor.State> implements Parent
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
        public Task<Integer> localNesting(int i1, int i2)
        {
            // Start Transaction
            return transaction(() ->
            {
                state().incrementBalance(i1);
                await(transaction(() ->
                {
                    state().incrementBalance(i2);
                    if (i2 < 0)
                    {
                        throw new IllegalArgumentException("i2: " + i2);
                    }
                    return Task.done();
                }));
                if (i1 < 0)
                {
                    // the nested transaction was completed
                    // this will force the parent to cancel the nested
                    throw new IllegalArgumentException("i1: " + i1);
                }
                return Task.fromValue(state().balance);
            });
            // End Transaction
        }

        @Override
        public Task<Integer> remoteNested(int i1, Parent other, int i2)
        {

            // Start Transaction
            return transaction(() ->
            {
                state().incrementBalance(i1);
                final int r1 = await(other.singleLevel(i2));
                if (i1 < 0)
                {
                    // the nested transaction was completed
                    // this will force the parent to cancel the nested
                    throw new IllegalArgumentException("i1: " + i1);
                }
                return Task.fromValue(state().balance + r1);
            });
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
        public Task<Void> cancelTransaction(final String transactionId)
        {
            return super.cancelTransaction(transactionId);
        }
    }

    @Test
    public void nestingSuccessful() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        assertEquals(3, (int) jimmy.localNesting(1, 2).join());
    }

    @Test
    public void nestingFailureInNested() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        assertEquals(3, (int) jimmy.localNesting(1, 2).join());
        assertEquals("i2: -2", expectException(() -> jimmy.localNesting(1, -2).join()).getCause().getMessage());
        eventually(() -> assertEquals(3, (int) jimmy.getBalance().join()));
    }

    @Test
    public void nestingFailureInOuter() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        assertEquals(3, (int) jimmy.localNesting(1, 2).join());
        assertEquals("i1: -1", expectException(() -> jimmy.localNesting(-1, 2).join()).getCause().getMessage());
        eventually(() -> assertEquals(3, (int) jimmy.getBalance().join()));
        dumpMessages();
    }

    @Test
    public void remoteNesting() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        Parent other = Actor.getReference(Parent.class, "other");
        assertEquals(3, (int) jimmy.remoteNested(1, other, 2).join());
        assertEquals(1, (int) jimmy.getBalance().join());
        assertEquals(2, (int) other.getBalance().join());
    }

    @Test
    public void remoteNestingFailureInNested() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        Parent other = Actor.getReference(Parent.class, "other");
        assertEquals(3, (int) jimmy.remoteNested(1, other, 2).join());

        assertEquals("i: -2", expectException(() -> jimmy.remoteNested(1, other, -2).join()).getCause().getMessage());

        assertEquals(1, (int) jimmy.getBalance().join());
        assertEquals(2, (int) other.getBalance().join());
    }

    @Test
    public void remoteNestingFailureInOuter() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Parent jimmy = Actor.getReference(Parent.class, "1");
        Parent other = Actor.getReference(Parent.class, "other");
        // a successful transaction
        assertEquals(3, (int) jimmy.remoteNested(1, other, 2).join());

        // the failure
        // the outer transaction will fail after the nested was completed
        // this will cause the nested to be cancelled.
        assertEquals("i1: -1", expectException(() -> jimmy.remoteNested(-1, other, 2).join()).getCause().getMessage());

        assertEquals(1, (int) jimmy.getBalance().join());
        assertEquals(2, (int) other.getBalance().join());
        dumpMessages();
    }

}

