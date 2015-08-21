package com.ea.orbit.actors.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.concurrent.Task;

public interface Transaction extends Actor
{
    @OneWay
    Task<Void> initTransaction(String parentTransactionId, Actor initiator);

    @OneWay
    Task<Void> registerActor(Actor actor);

    @OneWay
    Task<Void> registerNestedTransaction(Transaction child, final Actor initiator);

    @OneWay
    Task<Void> cancelTransaction(final Actor caller);

    @OneWay
    Task<Void> transactionSuccessful();
}
