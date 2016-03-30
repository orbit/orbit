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

package cloud.orbit.actors.transactions;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static cloud.orbit.actors.transactions.TransactionUtils.transaction;
import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class EventPersistenceTest extends ActorBaseTest
{
    public interface Jimmy extends Actor
    {
        Task<String> wave(int amount);

        Task<String> wave(float amount);

        Task<String> wave(double amount);

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
            void incrementBalanceWithDouble(double amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            final Task<String> ret = transaction(() ->
            {
                // call method
                // register call
                state().incrementBalance(amount);
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
        public Task<String> wave(float amount)
        {
            // Start Transaction
            return transaction(() ->
            {
                // call method
                // register call
                state().incrementBalanceWithFloat(amount);
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            // End Transaction
        }

        @Override
        public Task<String> wave(double amount)
        {
            // Start Transaction
            return transaction(() ->
            {
                // call method
                // register call
                state().incrementBalanceWithDouble(amount);
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            // End Transaction
        }


        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }
    }

    @Test
    public void simpleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6).join();
        stage.stop().join();
        Stage stage2 = createStage();
        jimmy.wave(6).join();
        assertEquals(12, (int) jimmy.getBalance().join());
    }

    @Test
    public void simpleFloatTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6.1f).join();
    }


    @Test
    public void simpleDoubleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6.2d).join();
    }

    @Test
    public void transactionRollback() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        String t1 = jimmy.wave(6).join();

        assertEquals(6, (int) jimmy.getBalance().join());

        String t2 = jimmy.wave(60).join();
        String t3 = jimmy.wave(600).join();

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t2).join();
        eventually(() -> assertEquals(606, (int) jimmy.getBalance().join()));

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t1).join();
        eventually(() -> assertEquals(600, (int) jimmy.getBalance().join()));
    }

}
