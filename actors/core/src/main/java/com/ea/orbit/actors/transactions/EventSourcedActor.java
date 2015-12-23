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
import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.runtime.ActorState;
import com.ea.orbit.concurrent.Task;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.ea.async.Async.await;

public class EventSourcedActor<T extends TransactionalState> extends AbstractActor<T> implements Transactional, Actor
{
    @Override
    protected void createDefaultState()
    {
        super.createDefaultState();
    }

    @Override
    public Object interceptStateMethod(
            final Method method,
            final String event, final Object[] args)
    {
        if (method.isAnnotationPresent(TransactionalEvent.class))
        {
            final String transactionId = TransactionUtils.currentTransactionId();
            if (transactionId != null
                    // and there is no previous transaction
                    && (state().events.size() == 0
                    // or is not the last transaction
                    || !transactionId.equals(state().events.get(state().events.size() - 1).getTransactionId())))
            {
                final Task<Void> notificationTask = Actor.getReference(Transaction.class, transactionId).registerActor(EventSourcedActor.this);

                // when the TransactionalEvent returns CompletableFuture
                // it's assumed that the application wants to ensure that the transaction was notified.
                if (CompletableFuture.class.isAssignableFrom(method.getReturnType()))
                {
                    return notificationTask.thenCompose(() ->
                    {
                        state().events.add(
                                new TransactionEvent(
                                        ActorRuntime.getRuntime().clock().millis(),
                                        transactionId, event, args));
                        return (CompletableFuture) super.interceptStateMethod(method, event, args);
                    });
                }
            }
            state().events.add(
                    new TransactionEvent(
                            ActorRuntime.getRuntime().clock().millis(),
                            transactionId, event, args));
        }
        return super.interceptStateMethod(method, event, args);
    }

    public Task<Void> cancelTransaction(String transactionId)
    {
        List<TransactionEvent> newList = new ArrayList<>();

        // reset state
        List<TransactionEvent> events = state().events;
        createDefaultState();

        for (TransactionEvent event : events)
        {
            if (event.getTransactionId() == null || !event.getTransactionId().equals(transactionId))
            {
                ActorState state = (ActorState) state();
                final Object ret = state.invokeEvent(event.getMethodName(), event.getParams());
                if (ret instanceof CompletableFuture)
                {
                    await((CompletableFuture) ret);
                }
                newList.add(event);
            }
        }

        state().events = newList;

        return Task.done();
    }

}
