/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.test.samples;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

public class DemoTest extends ActorBaseTest
{

    public interface Bank extends Actor
    {
        Task add(int value);

        Task<Integer> getBalance();

        Task remove(int value);
    }

    public interface Inventory extends Actor
    {
        Task buy(int value, String item);
    }

    public static class BankActor extends AbstractActor<BankActor.State> implements Bank
    {
        public static class State
        {
            int balance;
        }

        @Override
        public Task<?> activateAsync()
        {
            getLogger().info("bank activated!");
            return super.activateAsync();
        }

        @Override
        public Task<?> deactivateAsync()
        {
            getLogger().info("bank deactivated!!!");
            return super.deactivateAsync();
        }

        @Override
        public Task add(final int value)
        {
            getLogger().info("adding: " + value);
            state().balance += value;
            return writeState();
        }

        @Override
        public Task<Integer> getBalance()
        {
            getLogger().info("getBalance: " + state().balance);
            return Task.fromValue(state().balance);
        }

        @Override
        public Task remove(final int value)
        {
            state().balance -= value;
            return writeState();
        }
    }

    public static class InventoryActor extends AbstractActor<InventoryActor.State> implements Inventory
    {
        public static class State
        {
            List<String> items = new ArrayList<>();
        }

        @Override
        public Task buy(final int value, final String item)
        {
            final Bank bank = Actor.getReference(Bank.class, getIdentity());
            await(bank.remove(value));
            state().items.add(item);
            return writeState();
        }
    }


    @Test
    public void actorDemo() throws ExecutionException, InterruptedException
    {
        clock.stop();
        Stage stage1 = createStage();
        Stage stage2 = createStage();
        Bank bank = Actor.getReference(Bank.class, "ak");
        bank.add(1000).join();
        assertEquals(1000, bank.getBalance().join().intValue());

        final Inventory inventory = Actor.getReference(Inventory.class, "ak");
        inventory.buy(900, "food").join();
        assertEquals(100, bank.getBalance().join().intValue());
        clock.incrementTime(11, TimeUnit.MINUTES);

        loggerExtension.addToSequenceDiagram("...10 minutes later...");
        stage2.cleanup().join();
        stage1.cleanup().join();
        stage1.stop().join();
        stage2.bind();
        assertEquals(100, bank.getBalance().join().intValue());

        dumpMessages();
    }

}
