package com.ea.orbit.actors.transactions;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.runtime.ActorRuntime;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;

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
