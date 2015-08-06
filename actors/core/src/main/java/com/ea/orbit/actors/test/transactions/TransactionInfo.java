package com.ea.orbit.actors.test.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.concurrent.ConcurrentHashSet;

import java.util.Date;
import java.util.Set;

public class TransactionInfo
{
    String transactionId;
    Date transactionDate = new Date();
    Set<Actor> messagedActors = new ConcurrentHashSet<>();
    Set<String> subTransactions = new ConcurrentHashSet<>();
}