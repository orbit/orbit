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
    @Override
    public Task<Void> registerActorInvolvement(final Actor actor)
    {
        if (state().canceled)
        {
            // fires the cancellation back to this actor without waiting for the return
            // important!: waiting could cause a deadlock
            Actor.cast(TransactionalAware.class, actor).cancelTransaction(this.actorIdentity());
        }
        state().actors.add(actor);
        writeState();
        return Task.done();
    }

    @Override
    public Task<Void> registerSubTransaction(final Transaction child)
    {
        state().children.add(child);
        return Task.done();
    }

    @Override
    public Task<Void> cancelTransaction(final Actor caller)
    {
        if (!state().canceled)
        {
            final Task<Void> members = Task.allOf(state().actors.stream()
                    .filter(actor -> !actor.equals(caller))
                    .map(a -> Actor.cast(TransactionalAware.class, a).cancelTransaction(this.actorIdentity())));

            final Task<Void> subs = Task.allOf(state().children.stream()
                    .map(a -> a.cancelTransaction(caller)));

            await(Task.allOf(members, subs));
        }
        return Task.done();
    }

    @Override
    public Task<Void> initTransaction(final String parentTransactionId, final Actor initiator)
    {
        state().parentTransactionId = parentTransactionId;
        if (parentTransactionId != null)
        {
            await(Actor.getReference(Transaction.class, parentTransactionId).registerSubTransaction(this));
        }
        return writeState();
    }

    public static class TransactionState
    {
        Set<Actor> actors = new LinkedHashSet<>();
        Set<Transaction> children = new LinkedHashSet<>();
        String parentTransactionId;
        boolean canceled;
        long startTime = ActorRuntime.getRuntime().clock().millis();
    }
}
