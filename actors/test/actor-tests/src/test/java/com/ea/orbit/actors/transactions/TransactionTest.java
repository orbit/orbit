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
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.actors.test.FakeSync;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.ea.orbit.actors.transactions.TransactionUtils.transaction;
import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.*;

/**
 *
 */
public class TransactionTest extends ActorBaseTest
{
    public interface Bank extends Actor
    {
        Task<Integer> decrement(int amount);

        Task<Integer> getBalance();

        Task<Integer> increment(int amount);
    }

    public static class BankActor extends EventSourcedActor<BankActor.State> implements Bank
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionalEvent
            void decrementBalance(int amount)
            {
                balance -= amount;
            }

            @TransactionalEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<Integer> decrement(int amount)
        {
            if (state().balance < amount)
            {
                throw new IllegalArgumentException("not enough funds: " + amount + " > " + state().balance);
            }
            state().decrementBalance(amount);
            return getBalance();
        }

        @Override
        public Task<Integer> increment(int amount)
        {
            state().incrementBalance(amount);
            return getBalance();
        }

        @Override
        public Task<Integer> getBalance()
        {
            return Task.fromValue(state().balance);
        }
    }

    public interface Inventory extends Actor
    {
        Task<String> giveItem(String itemName);

        Task<List<String>> getItems();
    }

    public static class InventoryActor
            extends EventSourcedActor<InventoryActor.State>
            implements Inventory
    {
        // test synchronization
        @Inject
        FakeSync fakeSync;

        public static class State extends TransactionalState
        {
            List<String> items = new ArrayList<>();

            @TransactionalEvent
            void addItem(String item)
            {
                items.add(item);
            }
        }

        @Override
        public Task<String> giveItem(String itemName)
        {
            if (!"ok".equals(fakeSync.get("proceed").join()))
            {
                throw new IllegalArgumentException("Something went wrong: " + itemName);
            }

            String item = itemName + ":" + UUID.randomUUID().toString();
            state().addItem(item);
            return Task.fromValue(item);
        }


        @Override
        public Task<List<String>> getItems()
        {
            return Task.fromValue(state().items);
        }
    }


    public interface TtStore extends Actor
    {
        Task<String> buyItem(Bank bank, Inventory inventory, String itemName, int price);
    }

    public static class StoreActor
            extends EventSourcedActor<StoreActor.State>
            implements TtStore
    {
        public static class State extends TransactionalState
        {

        }

        @Override
        public Task<String> buyItem(Bank bank, Inventory inventory, String itemName, int price)
        {
            return transaction(() ->
            {
                final Task<Integer> decrement = bank.decrement(price);
                final Task<String> stringTask = inventory.giveItem(itemName);
                await(decrement);
                return stringTask;
            });
        }
    }

    @Test
    public void successfulTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        TtStore store = Actor.getReference(TtStore.class, "all");
        bank.increment(15).join();

        // this allows the store to proceed without blocking
        fakeSync.put("proceed", "ok");
        store.buyItem(bank, inventory, "candy", 10).join();
        // got the item
        assertTrue(inventory.getItems().join().get(0).startsWith("candy:"));
        // the balance was decreased
        assertEquals((Integer) 5, bank.getBalance().join());
        Thread.sleep(1000);
        dumpMessages();
    }

    @Test
    public void failedTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        TtStore store = Actor.getReference(TtStore.class, "all");
        bank.increment(15).join();

        fakeSync.put("proceed", "fail");

        // this item is invalid but the bank balance is decreased first.
        expectException(() -> store.buyItem(bank, inventory, "ice cream", 10).join());

        // the bank credits must be restored.
        eventually(() -> assertEquals((Integer) 15, bank.getBalance().join()));
        // no items were given
        assertEquals(0, inventory.getItems().join().size());
        dumpMessages();
    }

    @Test
    public void failedTransaction2() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        TtStore store = Actor.getReference(TtStore.class, "all");
        bank.increment(15).join();

        fakeSync.put("proceed", "ok");

        // this item is invalid but the bank balance is decreased first.
        store.buyItem(bank, inventory, "candy", 10).join();
        store.buyItem(bank, inventory, "chocolate", 1).join();
        try
        {
            store.buyItem(bank, inventory, "ice cream", 50).join();
            fail("expecting an exception");
        }
        catch (CompletionException ex)
        {
            System.out.println(ex.getCause().getMessage());
        }

        // the bank credits must be restored.
        eventually(() -> assertEquals((Integer) 4, bank.getBalance().join()));
        // no items were given
        eventually(() -> assertEquals(2, inventory.getItems().join().size()));
        dumpMessages();
    }


}

