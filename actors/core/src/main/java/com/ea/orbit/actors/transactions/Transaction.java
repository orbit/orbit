package com.ea.orbit.actors.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.Task;

public interface Transaction extends Actor
{
    Task<Void> registerActorInvolvement(Actor actor);

    Task<Void> registerSubTransaction(Transaction child);

    Task<Void> cancelTransaction(final Actor caller);

    Task<Void> initTransaction(String parentTransactionId, Actor initiator);
}
