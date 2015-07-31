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

package com.ea.orbit.actors.test.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TransactionTest extends ActorBaseTest
{
    public interface Jimmy extends TransactionalActor
    {
        Task<String> wave(int amount);

        Task<Integer> getBalance();
    }

    public interface TransactionalActor extends Actor
    {
        Task<Void> cancelTransaction(String transactionId);
    }

    public static class AbstractTransactionalActor<T extends TransactionalState> extends AbstractActor<T> implements TransactionalActor
    {

        static ThreadLocal<String> currentTransactionId = new ThreadLocal<>();

        protected <T> Task<T> transaction(Function<String, Task<T>> function)
        {
            String transactionId = UUID.randomUUID().toString();
            currentTransactionId.set(transactionId);
            return function.apply(transactionId);
        }

        public Task<Void> cancelTransaction(String transactionId)
        {
            List<TransactionEvent> newList = new ArrayList<>();

            // reset state

            List<TransactionEvent> events = state().events;
            createDefaultState();

            for (TransactionEvent event : events)
            {
                if (!event.getTransactionId().equals(transactionId))
                {
                    // call this
                    //incrementBalance((Integer)event.getParams()[0]);
                    try
                    {
                        Stream.of(getClass().getDeclaredMethods())
                                .filter(method -> method.getName().equals(event.getMethodName()))
                                .findFirst()
                                .get()
                                .invoke(this, event.getParams());
                    } catch (IllegalAccessException | InvocationTargetException e)
                    {
                        throw new UncheckedException(e);
                    }

                    newList.add(event);
                }
            }

            state().events = newList;

            return Task.done();
        }


    }

    public static class JimmyActor extends AbstractTransactionalActor<JimmyActor.State> implements Jimmy
    {
        public static class State extends TransactionalState
        {
            int balance;

        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            Task<String> res = transaction(t -> {
                // call method
                // register call
               // state().events.add(new TransactionEvent(currentTransactionId.get(), "incrementBalance", amount));
                incrementBalance(amount);
                return Task.fromValue(t);
            });
            // End Transaction
            return res;
        }


        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }

        @TransactionSafeEvent
        void incrementBalance(int amount)
        {
            state().balance += amount;
        }
    }

    @Test
    public void simpleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6).join();
    }

    @Test
    public void transactionRollback() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        String t1 = jimmy.wave(6).join();
        String t2 = jimmy.wave(60).join();
        String t3 = jimmy.wave(600).join();

        jimmy.cancelTransaction(t2);
        assertEquals(606, (int) jimmy.getBalance().join());

        jimmy.cancelTransaction(t1);
        assertEquals(600, (int) jimmy.getBalance().join());
    }

}
