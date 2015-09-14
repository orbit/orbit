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
public class EventPersistenceTest extends ActorBaseTest
{
    public interface Jimmy extends Actor
    {
        Task<String> wave(int amount);

        Task<String> wave(float amount);

        Task<String> wave(double amount);

        Task<Integer> getBalance();
    }

    public static class JimmyActor extends EventSourcedActor<JimmyActor.State> implements Jimmy
    {
        public static class State extends TransactionalState
        {
            int balance;

            @TransactionalEvent
            void incrementBalance(int amount)
            {
                balance += amount;
            }

            @TransactionalEvent
            void incrementBalanceWithFloat(float amount)
            {
                balance += amount;
            }

            @TransactionalEvent
            void incrementBalanceWithDouble(double amount)
            {
                balance += amount;
            }
        }

        @Override
        public Task<String> wave(int amount)
        {
            // Start Transaction
            final Task<String> ret = transaction(() ->
            {
                // call method
                // register call
                state().incrementBalance(amount);
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            await(ret);
            // End Transaction
//            await(writeState());
            return ret;
        }

        @Override
        public Task<String> wave(float amount)
        {
            // Start Transaction
            return transaction(() ->
            {
                // call method
                // register call
                state().incrementBalanceWithFloat(amount);
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            // End Transaction
        }

        @Override
        public Task<String> wave(double amount)
        {
            // Start Transaction
            return transaction(() ->
            {
                // call method
                // register call
                state().incrementBalanceWithDouble(amount);
                if (amount < 0)
                {
                    throw new IllegalArgumentException("Illegal value: " + amount);
                }
                return Task.fromValue(TransactionUtils.currentTransactionId());
            });
            // End Transaction
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
        stage.stop().join();
        Stage stage2 = createStage();
        jimmy.wave(6).join();
        //assertEquals(12, (int) jimmy.getBalance().join());


    }

    @Test
    public void simpleFloatTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6.1f).join();
    }


    @Test
    public void simpleDoubleTransaction() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();

        Jimmy jimmy = Actor.getReference(Jimmy.class, "1");
        jimmy.wave(6.2d).join();
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

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t2).join();
        eventually(() -> assertEquals(606, (int) jimmy.getBalance().join()));

        Actor.cast(Transactional.class, jimmy).cancelTransaction(t1).join();
        eventually(() -> assertEquals(600, (int) jimmy.getBalance().join()));
    }


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


    public interface Store extends Actor
    {
        Task<String> buyItem(Bank bank, Inventory inventory, String itemName, int price);
    }

    public static class StoreActor
            extends EventSourcedActor<StoreActor.State>
            implements Store
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
        Store store = Actor.getReference(Store.class, "all");
        bank.increment(15).join();

        // this allows the store to proceed without blocking
        fakeSync.put("proceed", "ok");
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

        fakeSync.put("proceed", "fail");

        // this item is invalid but the bank balance is decreased first.
        expectException(() -> store.buyItem(bank, inventory, "ice cream", 10).join());

        // the bank credits must be restored.
        eventually(() -> assertEquals((Integer) 15, bank.getBalance().join()));
        // no items were given
        eventually(() -> assertEquals(0, inventory.getItems().join().size()));
        dumpMessages();
    }

    @Test
    public void failedTransaction2() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        Bank bank = Actor.getReference(Bank.class, "jimmy");
        Inventory inventory = Actor.getReference(Inventory.class, "jimmy");
        Store store = Actor.getReference(Store.class, "all");
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
    }


    public interface EpTransactionTree extends Actor
    {
        Task<Integer> treeInc(int count);

        Task<Integer> treeTransaction(int count);

        Task<Integer> currentValue();
    }

    public static class TransactionTreeActor
            extends EventSourcedActor<TransactionTreeActor.State>
            implements EpTransactionTree
    {
        public static class State extends TransactionalState
        {
            int current;

            @TransactionalEvent
            int incAngGet(int count)
            {
                return current += count;
            }
        }

        @Override
        public Task<Integer> treeTransaction(int count)
        {
            return transaction(() ->
                    treeInc(count));
        }

        @Override
        public Task<Integer> currentValue()
        {
            return Task.fromValue(state().current);
        }

        @Override
        public Task<Integer> treeInc(int count)
        {
            final String id = actorIdentity();
            if (id.length() == 1)
            {
                if (id.equals("x"))
                {
                    throw new RuntimeException("Expected exception");
                }
                return Task.fromValue(state().incAngGet(count));
            }
            final int mid = id.length() / 2;
            final EpTransactionTree left = Actor.getReference(EpTransactionTree.class, id.substring(0, mid));
            final EpTransactionTree right = Actor.getReference(EpTransactionTree.class, id.substring(mid));
            final Task<Integer> ltask = left.treeInc(count);
            final Task<Integer> rtask = right.treeInc(count);
            return Task.fromValue(await(ltask) + await(rtask));
        }
    }


    @Test
    public void treeSuccess() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        EpTransactionTree tree = Actor.getReference(EpTransactionTree.class, "abc");
        assertEquals((Integer) 3, tree.treeTransaction(1).join());
        assertEquals((Integer) 1, Actor.getReference(EpTransactionTree.class, "a").currentValue().join());
        assertEquals((Integer) 1, Actor.getReference(EpTransactionTree.class, "b").currentValue().join());
        assertEquals((Integer) 1, Actor.getReference(EpTransactionTree.class, "c").currentValue().join());
    }


    @Test
    public void treeCancellation() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        EpTransactionTree tree2 = Actor.getReference(EpTransactionTree.class, "abcx");
        EpTransactionTree aleaf = Actor.getReference(EpTransactionTree.class, "a");
        assertEquals((Integer) 5, Actor.getReference(EpTransactionTree.class, "a").treeInc(5).join());
        expectException(() -> tree2.treeTransaction(1).join());
        eventually(() -> assertEquals((Integer) 5, Actor.getReference(EpTransactionTree.class, "a").currentValue().join()));
        eventually(() -> assertEquals((Integer) 0, Actor.getReference(EpTransactionTree.class, "b").currentValue().join()));
        eventually(() -> assertEquals((Integer) 0, Actor.getReference(EpTransactionTree.class, "c").currentValue().join()));
        dumpMessages();
    }

}
