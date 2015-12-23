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
import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class EventTypeTest extends ActorBaseTest
{
    public interface Jimmy extends Actor
    {
        Task<String> wave(int amount);

        Task<Integer> getBalance();
    }

    public static class JimmyActor extends EventSourcedActor<JimmyActor.State> implements Jimmy
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionalEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }

            @TransactionalEvent
            void incrementBalanceWithFloat(float amount)
            {
                balance += amount;
            }

            @TransactionalEvent
            int returnInt()
            {
                return balance;
            }

            @TransactionalEvent
            double returnDouble()
            {
                return balance;
            }

            @TransactionalEvent
            long returnLong()
            {
                return balance;
            }

            @TransactionalEvent
            Long returnWLong()
            {
                return new Long(balance);
            }

            @TransactionalEvent
            Object returnObject()
            {
                return new Long(balance);
            }

            @TransactionalEvent
            Task returnTask()
            {
                return Task.fromValue(balance);
            }
        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            final Task<String> ret = transaction(() ->
            {
                state().incrementBalance(amount);
                state().incrementBalanceWithFloat(amount);
                state().returnInt();
                state().returnDouble();
                state().returnLong();
                state().returnWLong();
                state().returnObject();
                await(state().returnTask());
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            await(ret);
            // End Transaction
            await(writeState());
            return ret;
        }

        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }
    }

    @Test
    public void exercise() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(3).join();
        assertEquals(6, (int) jimmy.getBalance().join());
        final String tid = jimmy.wave(5).join();
        assertEquals(16, (int) jimmy.getBalance().join());
        jimmy.wave(7).join();
        assertEquals(30, (int) jimmy.getBalance().join());
        Actor.cast(Transactional.class, jimmy).cancelTransaction(tid).join();
        eventually(500, () -> assertEquals(20, (int) jimmy.getBalance().join()));
    }


    @Test
    public void exerciseWithPersistence() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(3).join();
        assertEquals(6, (int) jimmy.getBalance().join());
        final String tid = jimmy.wave(5).join();
        assertEquals(16, (int) jimmy.getBalance().join());
        stage.stop().join();

        Stage stage2 = createStage();
        jimmy.wave(7).join();
        assertEquals(30, (int) jimmy.getBalance().join());
        System.out.println(fakeDatabase);
        Actor.cast(Transactional.class, jimmy).cancelTransaction(tid).join();
        eventually(500, () -> assertEquals(20, (int) jimmy.getBalance().join()));
    }


}
