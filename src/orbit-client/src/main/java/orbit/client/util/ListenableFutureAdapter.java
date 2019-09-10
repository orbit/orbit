/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.util;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

public class ListenableFutureAdapter<T> {
    private final CompletableFuture<T> completableFuture;

    @SuppressWarnings("UnstableApiUsage")
    private ListenableFutureAdapter(ListenableFuture<T> listenableFuture) {
        this.completableFuture = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(cancelled);
                return cancelled;
            }
        };

        Futures.addCallback(listenableFuture, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable ex) {
                completableFuture.completeExceptionally(ex);
            }
        }, Concurrent.orbitExecutor);
    }

    private CompletableFuture<T> getCompletableFuture() {
        return completableFuture;
    }

    public static <T> CompletableFuture<T> toCompletable(ListenableFuture<T> listenableFuture) {
        ListenableFutureAdapter<T> listenableFutureAdapter = new ListenableFutureAdapter<>(listenableFuture);
        return listenableFutureAdapter.getCompletableFuture();
    }

}