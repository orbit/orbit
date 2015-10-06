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
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.ea.orbit.actors.transactions.TransactionUtils.transaction;
import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TreeTransactionTest extends ActorBaseTest
{
    public interface TtTransactionTree extends Actor
    {
        Task<Integer> treeInc(int count);

        Task<Integer> treeTransaction(int count);

        Task<Integer> currentValue();
    }

    public static class TransactionTreeActor
            extends EventSourcedActor<TransactionTreeActor.State>
            implements TtTransactionTree
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
            final TtTransactionTree left = Actor.getReference(TtTransactionTree.class, id.substring(0, mid));
            final TtTransactionTree right = Actor.getReference(TtTransactionTree.class, id.substring(mid));
            final Task<Integer> ltask = left.treeInc(count);
            final Task<Integer> rtask = right.treeInc(count);
            return Task.fromValue(await(ltask) + await(rtask));
        }
    }


    @Test
    public void treeSuccess() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        TtTransactionTree tree = Actor.getReference(TtTransactionTree.class, "abc");
        assertEquals((Integer) 3, tree.treeTransaction(1).join());
        assertEquals((Integer) 1, Actor.getReference(TtTransactionTree.class, "a").currentValue().join());
        assertEquals((Integer) 1, Actor.getReference(TtTransactionTree.class, "b").currentValue().join());
        assertEquals((Integer) 1, Actor.getReference(TtTransactionTree.class, "c").currentValue().join());
    }


    @Test
    public void treeCancellation() throws ExecutionException, InterruptedException
    {
        Stage stage = createStage();
        TtTransactionTree tree2 = Actor.getReference(TtTransactionTree.class, "abcx");
        TtTransactionTree aleaf = Actor.getReference(TtTransactionTree.class, "a");
        assertEquals((Integer) 5, Actor.getReference(TtTransactionTree.class, "a").treeInc(5).join());
        expectException(() -> tree2.treeTransaction(1).join());
        eventually(() -> assertEquals((Integer) 5, Actor.getReference(TtTransactionTree.class, "a").currentValue().join()));
        assertEquals((Integer) 0, Actor.getReference(TtTransactionTree.class, "b").currentValue().join());
        assertEquals((Integer) 0, Actor.getReference(TtTransactionTree.class, "c").currentValue().join());
        dumpMessages();
    }

}

