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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.actors.test.FakeSync;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.junit.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TransactionTest extends ActorBaseTest
{
    public interface Jimmy extends TransactionalActor
    {
        Task<String> wave(int amount);

        Task<Integer> getBalance();
    }

    public static class JimmyActor extends AbstractTransactionalActor<JimmyActor.State> implements Jimmy
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionSafeEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            Task<String> res = transaction(t -> {
                // call method
                // register call
                state().events.add(new TransactionEvent(currentTransactionId(), "incrementBalance", amount));
                state().incrementBalance(amount);
                return Task.fromValue(t);
            });
            // End Transaction
            return res;
        }

        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }
    }

    @Test
    public void simpleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6).join();
    }

    @Test
    public void transactionRollback() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        String t1 = jimmy.wave(6).join();

        assertEquals(6, (int) jimmy.getBalance().join());

        String t2 = jimmy.wave(60).join();
        String t3 = jimmy.wave(600).join();

        jimmy.cancelTransaction(t2).join();
        assertEquals(606, (int) jimmy.getBalance().join());

        jimmy.cancelTransaction(t1).join();
        assertEquals(600, (int) jimmy.getBalance().join());
    }


    public interface Bank extends TransactionalActor
    {
        Task<Integer> decrement(int amount);

        Task<Integer> getBalance();

        Task<Integer> increment(int amount);
    }

    public static class BankActor extends AbstractTransactionalActor<BankActor.State> implements Bank
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionSafeEvent
            void decrementBalance(int amount)
            {
                balance -= amount;
            }

            @TransactionSafeEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<Integer> decrement(int amount)
        {
            state().events.add(new TransactionEvent(currentTransactionId(), "decrementBalance", amount));
            state().decrementBalance(amount);
            return getBalance();
        }

        @Override
        public Task<Integer> increment(int amount)
        {
            state().events.add(new TransactionEvent(currentTransactionId(), "incrementBalance", amount));
            state().incrementBalance(amount);
            return getBalance();
        }

        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }
    }

    public interface Inventory extends TransactionalActor
    {
        Task<String> giveItem(String itemName);

        Task<List<String>> getItems();
    }

    public static class InventoryActor
            extends AbstractTransactionalActor<InventoryActor.State>
            implements Inventory
    {
        public static class State extends TransactionalState
        {
            List<String> items = new ArrayList<>();

            @TransactionSafeEvent
            void addItem(String item)
            {
                items.add(item);
            }
        }

        @Override
        public Task<String> giveItem(String itemName)
        {
            String item = itemName + ":" + UUID.randomUUID().toString();
            state().events.add(new TransactionEvent(currentTransactionId(), "addItem", item));
            state().addItem(item);
            return Task.fromValue(item);
        }


        @Override
        public Task<List<String>> getItems()
        {
            return Task.fromValue(state().items);
        }
    }


    public interface Store extends TransactionalActor
    {
        Task<String> buyItem(Bank bank, Inventory inventory, String itemName, int price);
    }

    public static class StoreActor
            extends AbstractTransactionalActor<StoreActor.State>
            implements Store
    {
        // test synchronization
        @Inject
        FakeSync fakeSync;

        public static class State extends TransactionalState
        {

        }

        @Override
        public Task<String> buyItem(Bank bank, Inventory inventory, String itemName, int price)
        {
            return transaction(t -> {
                await(bank.decrement(price));
                fakeSync.get("proceed").join();
                return inventory.giveItem(itemName);
            }).handle((r, e) ->
            {
                if (e != null)
                {
                    final String transactionId = currentTransactionId();
                    await(Task.allOf(bank.cancelTransaction(transactionId),
                            inventory.cancelTransaction(transactionId)));
                    throw new UncheckedException(e);
                }
                return r;
            });
        }
    }

    @Test
    public void successfulTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        Store store = Actor.getReference(Store.class, "all");
        bank.increment(15).join();

        // this allows the store to proceed without blocking
        fakeSync.put("proceed", true);
        store.buyItem(bank, inventory, "candy", 10).join();
        // got the item
        assertTrue(inventory.getItems().join().get(0).startsWith("candy:"));
        // the balance was decreased
        assertEquals((Integer) 5, bank.getBalance().join());
    }

    @Test
    public void failedTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        Store store = Actor.getReference(Store.class, "all");
        bank.increment(15).join();

        fakeSync.putException("proceed", new UncheckedException("intentionally failing"));

        // this item is invalid but the bank balance is decreased first.
        expectException(() -> store.buyItem(bank, inventory, "ice cream", 10).join());

        // the bank credits must be restored.
        assertEquals((Integer) 15, bank.getBalance().join());
        // no items were given
        assertEquals(0, inventory.getItems().join().size());
    }


}