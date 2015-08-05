package com.ea.orbit.actors.test.transactions;

import com.ea.orbit.actors.Actor;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class TransactionInfo
{
    String transactionId;
    Date transactionDate;
    Set<Actor> messagedActors;
    Set<String> subTransactions = new HashSet<>();
}