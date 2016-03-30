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

package cloud.orbit.actors.transactions;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskContext;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static com.ea.async.Async.await;

public class TransactionUtils
{
    public static final String ORBIT_TRANSACTION_ID = "orbit.transactionId";


    static String currentTransactionId(final TaskContext context)
    {
        String tid = (String) context.getProperty(ORBIT_TRANSACTION_ID);
        if (tid != null)
        {
            return tid;
        }
        return null;
    }


    public static String currentTransactionId()
    {
        final TaskContext context = TaskContext.current();
        if (context == null)
        {
            return null;
        }
        return currentTransactionId(context);
    }


    /**
     * Asynchronous transaction
     *
     * @param body the transaction scoped code
     * @param <R>  the return type of the transaction
     * @return returns a promise of the transaction completion
     */
    public static <R> Task<R> transaction(Supplier<Task<R>> body)
    {
        final String parentTransactionId = currentTransactionId();

        final ActorTaskContext oldContext = ActorTaskContext.current();
        final ActorTaskContext context = oldContext != null ? oldContext.cloneContext() : new ActorTaskContext();


        // http://security.stackexchange.com/questions/1952/how-long-should-a-random-nonce-be
        final String transactionId = IdUtils.urlSafeString(96);

        context.setProperty(ORBIT_TRANSACTION_ID, transactionId);
        final Transaction transaction = Actor.getReference(Transaction.class, transactionId);
        // asynchronously initiating the transaction
        transaction.initTransaction(parentTransactionId, null);

        // mus not use await in this method after the push, since push and pop must be in the same thread.
        context.push();
        if (oldContext == null)
        {
            final ActorRuntime runtime = ActorRuntime.getRuntime();
            if (runtime != null)
            {
                runtime.bind();
            }
        }
        try
        {
            return safeCall(() -> {
                Task<R> result = safeCall(body);
                try
                {
                    // to catch the exception without CompletionStage contortions
                    await(result);
                    transaction.transactionSuccessful();
                }
                catch (Exception ex)
                {
                    final Throwable aex = (ex instanceof CompletionException) ? ex.getCause() : ex;
                    // asynchronously cancelling the transaction
                    transaction.cancelTransaction(null)
                            .thenCompose(() -> Task.fromException(aex));
                }
                return result;
            });
        }
        finally
        {
            context.pop();
        }
    }

    static final <T> Task<T> safeCall(Supplier<Task<T>> supplier)
    {
        try
        {
            final Task<T> task = supplier.get();
            return task != null ? task : Task.fromValue(null);
        }
        catch (Throwable ex)
        {
            return Task.fromException(ex);
        }
    }

}
