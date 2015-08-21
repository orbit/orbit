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
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;
import com.ea.orbit.exception.UncheckedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.ea.orbit.async.Await.await;

public class EventSourcedActor<T extends TransactionalState> extends AbstractActor<T> implements Transactional, Actor
{

    public static final String ORBIT_TRANSACTION_ID = "orbit.transactionId";

    public static String currentTransactionId()
    {
        final TaskContext context = TaskContext.current();
        if (context == null)
        {
            return null;
        }
        return currentTransactionId(context);
    }

    @Override
    protected void createDefaultState()
    {
        super.createDefaultState();
    }

    @Override
    protected Object interceptStateMethod(
            final Object self,
            final Method method,
            final Method proceed,
            final Object[] args)
            throws IllegalAccessException, InvocationTargetException
    {
        if (method.isAnnotationPresent(TransactionalEvent.class))
        {
            final String transactionId = currentTransactionId();
            if (transactionId != null)
            {
                Actor.getReference(Transaction.class, transactionId).registerActorInvolvement(EventSourcedActor.this);
            }
            state().events.add(new TransactionEvent(transactionId, proceed.getName(), args));
        }
        return super.interceptStateMethod(self, method, proceed, args);
    }

    static String currentTransactionId(final TaskContext context)
    {
        String tid = (String) context.getProperty(ORBIT_TRANSACTION_ID);
        if (tid != null)
        {
            return tid;
        }
        return null;
    }


    // this is error prone since the programmer might use awaits
    @Deprecated
    private <R> Task<R> transaction(Runnable runnable)
    {
        return transaction(() -> {
            runnable.run();
            return null;
        });
    }

    protected <R> Task<R> transaction(Supplier<Task<R>> function)
    {


        final String parentTransactionId = currentTransactionId();

        final ActorTaskContext oldContext = ActorTaskContext.current();
        final ActorTaskContext context = oldContext.cloneContext();

        final byte[] buf = new byte[12];
        // TODO, use secure random or some better id generator, btw, this is here just to ease debugging.
        new Random().nextBytes(buf);
        final String transactionId = Base64.getUrlEncoder().encodeToString(buf);

        context.setProperty(ORBIT_TRANSACTION_ID, transactionId);
        if (parentTransactionId != null)
        {
            final TransactionInfo parentTransaction = getOrAdTransactionInfo(parentTransactionId);
            parentTransaction.subTransactions.add(transactionId);
        }
        final TransactionInfo transaction = getOrAdTransactionInfo(transactionId);
        final Transaction transactionActor = Actor.getReference(Transaction.class, transactionId);
        await(transactionActor.initTransaction(parentTransactionId, this));

        // mus not use await in this method after the push, since push and pop must be in the same thread.
        context.push();
        try
        {
            return invokeTransaction(function, transactionId);
        }
        finally
        {
            context.pop();
        }
    }

    private <R> Task<R> invokeTransaction(final Supplier<Task<R>> function, final String transactionId)
    {
        Task<R> apply = null;
        try
        {
            apply = function.get();
        }
        catch (Exception ex)
        {
            final Throwable aex = (ex instanceof CompletionException) ? ex.getCause() : ex;
            cancelTransaction(transactionId);
            return Actor.getReference(Transaction.class, transactionId).cancelTransaction((Actor) this)
                    .thenCompose(() -> Task.fromException(ex));
        }
        if (apply != null && !(apply.isDone() && !apply.isCompletedExceptionally()))
        {
            if (apply.isCompletedExceptionally())
            {
                return apply;
            }
            try
            {
                // to catch the exception without CompletableFuture-FU contortions
                await(apply);
                return apply;
            }
            catch (Exception ex)
            {
                final Throwable aex = (ex instanceof CompletionException) ? ex.getCause() : ex;
                cancelTransaction(transactionId);
                return Actor.getReference(Transaction.class, transactionId).cancelTransaction(this)
                        .thenCompose(() -> Task.fromException(aex));
            }
        }
        return apply;
    }

    TransactionInfo getOrAdTransactionInfo(String transactionId)
    {
        final TransactionInfo info = state().transactions
                .stream().filter(t -> t.transactionId.equals(transactionId)).findFirst().orElse(null);
        if (info == null)
        {
            final TransactionInfo newInfo = new TransactionInfo();
            newInfo.transactionId = transactionId;
            state().transactions.add(newInfo);
            return newInfo;
        }
        return info;
    }

    public Task<Void> cancelTransaction(String transactionId)
    {
        List<TransactionEvent> newList = new ArrayList<>();

        // cancel nested first

        final TransactionInfo transactionInfo = getOrAdTransactionInfo(transactionId);
        for (String s : transactionInfo.subTransactions)
        {
            await(cancelTransaction(s));
        }


        // reset state
        List<TransactionEvent> events = state().events;
        List<TransactionInfo> transactions = state().transactions;
        createDefaultState();

        final Method[] declaredMethods = state().getClass().getDeclaredMethods();

        for (TransactionEvent event : events)
        {
            if (event.getTransactionId() == null || !event.getTransactionId().equals(transactionId))
            {
                try
                {
                    Stream.of(declaredMethods)
                            .filter(method -> method.getName().equals(event.getMethodName()))
                            .findFirst()
                            .get()
                            .invoke(state(), event.params());
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    throw new UncheckedException(e);
                }

                newList.add(event);
            }
        }

        try
        {
            Thread.sleep(200);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        transactions.remove(transactionInfo);
        state().events = newList;
        state().transactions = transactions;

//        return Task.allOf(transactionInfo.messagedActors.stream()
//                .filter(a -> a instanceof TransactionalAware)
//                .map(a -> ((TransactionalAware) a).cancelTransaction(transactionId))
//                .toArray(s -> new CompletableFuture[s]));
        return Task.done();
    }


}
