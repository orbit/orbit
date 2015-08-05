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

import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.actors.runtime.Messaging;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;
import com.ea.orbit.exception.UncheckedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.ea.orbit.async.Await.await;

public class AbstractTransactionalActor<T extends TransactionalState> extends AbstractActor<T> implements TransactionalActor
{

    public static final String ORBIT_TRANSACTION_ID = "orbit.transactionId";

    static String currentTransactionId()
    {
        final TaskContext context = TaskContext.current();
        if (context == null)
        {
            return null;
        }
        return currentTransactionId(context);
    }

    static String currentTransactionId(final TaskContext context)
    {
        String tid = (String) context.getProperty(ORBIT_TRANSACTION_ID);
        if (tid != null)
        {
            return tid;
        }
        final Object headers = context.getProperty(Messaging.ORBIT_MESSAGE_HEADERS);
        if (!(headers instanceof Map))
        {
            return null;
        }
        return (String) ((Map) headers).get(ORBIT_TRANSACTION_ID);
    }

    protected <R> Task<R> transaction(Function<String, Task<R>> function)
    {
        final String parentTransactionId = currentTransactionId();

        final ActorTaskContext oldContext = ActorTaskContext.current();
        final ActorTaskContext context = oldContext.cloneContext();

        final String transactionId = UUID.randomUUID().toString();

        context.setProperty(ORBIT_TRANSACTION_ID, transactionId);
        if (parentTransactionId != null)
        {
            final TransactionInfo parentTransaction = getOrAdTransactionInfo(parentTransactionId);
            parentTransaction.subTransactions.add(transactionId);
        }
        final TransactionInfo transaction = getOrAdTransactionInfo(transactionId);
        final Object headers = context.getProperty(Messaging.ORBIT_MESSAGE_HEADERS);
        final Map<String, Object> newHeaders = (headers instanceof Map) ? new LinkedHashMap<>((Map) headers) : new LinkedHashMap<>();
        newHeaders.put(ORBIT_TRANSACTION_ID, transactionId);
        context.setProperty(Messaging.ORBIT_MESSAGE_HEADERS, newHeaders);
        context.push();
        try
        {
            final Task<R> apply;
            apply = function.apply(transactionId);
            if (apply != null)
            {
                return apply.whenComplete((r, e) ->
                {
                    if (e != null)
                    {
                        cancelTransaction(transactionId);
                    }
                });
            }
            return apply;
        }
        catch (Exception ex)
        {
            await(cancelTransaction(transactionId));
            return Task.fromException(ex);
        }
        finally
        {
            context.pop();
        }
    }

    private TransactionInfo getOrAdTransactionInfo(String transactionId)
    {
        final TransactionInfo info = state().transactions
                .stream().filter(t -> t.transactionId.equals(transactionId)).findFirst().orElse(null);
        if (info != null)
        {
            final TransactionInfo newInfo = new TransactionInfo();
            newInfo.transactionId = transactionId;
            state().transactions.add(newInfo);
        }
        return info;
    }

    public Task<Void> cancelTransaction(String transactionId)
    {
        List<TransactionEvent> newList = new ArrayList<>();

        // reset state

        List<TransactionEvent> events = state().events;
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
                            .invoke(state(), event.getParams());
                }
                catch (IllegalAccessException | InvocationTargetException e)
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
