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
import com.ea.orbit.concurrent.Task;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.ea.orbit.async.Await.await;

/**
 * Represents a transaction
 */
public class TransactionActor extends AbstractActor<TransactionActor.TransactionState> implements Transaction
{
    public static class TransactionState
    {
        Set<Actor> actors = new LinkedHashSet<>();
        Set<Transaction> children = new LinkedHashSet<>();
        String parentTransactionId;
        boolean canceled;
        long startTime = ActorRuntime.getRuntime().clock().millis();
    }

    @Override
    public Task<Void> registerActor(final Actor actor)
    {
        state().actors.add(actor);
        if (state().canceled)
        {
            // fires the cancellation back to this actor without waiting for the return
            // important!: waiting could cause a deadlock
            Actor.cast(Transactional.class, actor).cancelTransaction(this.actorIdentity());
        }
        writeState();
        return Task.done();
    }

    @Override
    public Task<Void> registerNestedTransaction(final Transaction child, final Actor initiator)
    {
        state().children.add(child);
        if (state().canceled)
        {
            await(child.cancelTransaction(initiator));
        }
        return Task.done();
    }

    @Override
    public Task<Void> cancelTransaction(final Actor caller)
    {
        if (!state().canceled)
        {
            state().canceled = true;
            final Task<Void> members = Task.allOf(state().actors.stream()
                    //.filter(actor -> !actor.equals(caller))
                    .map(a -> Actor.cast(Transactional.class, a).cancelTransaction(this.actorIdentity())));

            final Task<Void> subs = Task.allOf(state().children.stream()
                    .map(a -> a.cancelTransaction(caller)));

            //await(Task.allOf(members, subs));
        }
        return Task.done();
    }

    @Override
    public Task<Void> initTransaction(final String parentTransactionId, final Actor initiator)
    {
        state().parentTransactionId = parentTransactionId;
        if (parentTransactionId != null)
        {
            Actor.getReference(Transaction.class, parentTransactionId).registerNestedTransaction(this, initiator);
        }
        return writeState();
    }

    @Override
    public Task<Void> transactionSuccessful()
    {
        return Task.done();
    }


}
