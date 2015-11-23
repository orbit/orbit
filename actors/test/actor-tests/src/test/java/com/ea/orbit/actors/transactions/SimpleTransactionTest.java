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
import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.ea.orbit.actors.transactions.TransactionUtils.currentTransactionId;
import static com.ea.orbit.actors.transactions.TransactionUtils.transaction;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SimpleTransactionTest extends ActorBaseTest
{
    public interface Player extends Actor
    {
        Task<String> wave(int amount);

        Task<Integer> getXp();

        Task<Void> incrementXp(int number);
    }

    public static class PlayerActor extends EventSourcedActor<PlayerActor.State> implements Player
    {
        public static class State extends TransactionalState
        {
            int zp;

            @TransactionalEvent
            void incrementXp(int amount)
            {
                zp += amount;
            }
        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            return transaction(() ->
            {
                // call method
                // register call
                state().incrementXp(amount);
                return Task.fromValue(currentTransactionId());
            });
            // End Transaction
        }

        @Override
        public Task<Integer> getXp()
        {
            return Task.fromValue(state().zp);
        }

        @Override
        public Task<Void> incrementXp(final int inc)
        {
            state().incrementXp(inc);
            if (inc <= 0)
            {
                throw new IllegalArgumentException("Invalid xp amount: " + inc);
            }
            return Task.done();
        }
    }

    @Test(timeout = 10_000L)
    public void simpleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Player jimmy = Actor.getReference(Player.class, "1");
        clearMessages();
        transaction(() -> {
            return jimmy.incrementXp(10);
        }).join();
        assertEquals(10, (int) jimmy.getXp().join());

        dumpMessages();
    }


    @Test(timeout = 10_000L)
    public void simpleTransactionFailure() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Player jimmy = Actor.getReference(Player.class, "1");
        jimmy.incrementXp(10).join();
        clearMessages();
        expectException(() -> transaction(() -> {
            return jimmy.incrementXp(-6);
        }).join());
        eventually(() -> assertEquals(10, (int)jimmy.getXp().join()));

        dumpMessages();
    }

    @Test
    public void transactionRollback() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Player jimmy = Actor.getReference(Player.class, "1");
        String t1 = jimmy.wave(6).join();

        assertEquals(6, (int) jimmy.getXp().join());

        String t2 = jimmy.wave(60).join();
        String t3 = jimmy.wave(600).join();

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t2).join();
        assertEquals(606, (int) jimmy.getXp().join());

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t1).join();
        assertEquals(600, (int) jimmy.getXp().join());
    }

}

