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
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.ea.orbit.async.Await.await;

public class EventSourcedActor<T extends TransactionalState> extends AbstractActor<T> implements Transactional, Actor
{
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
            final String transactionId = TransactionUtils.currentTransactionId();
            if (transactionId != null)
            {
                Actor.getReference(Transaction.class, transactionId).registerActor(EventSourcedActor.this);
            }
            state().events.add(new TransactionEvent(transactionId, proceed.getName(), args));
        }
        return super.interceptStateMethod(self, method, proceed, args);
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

        transactions.remove(transactionInfo);
        state().events = newList;
        state().transactions = transactions;

        return Task.done();
    }


}
