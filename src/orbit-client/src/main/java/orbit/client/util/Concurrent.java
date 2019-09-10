/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class Concurrent {
    public static Executor orbitExecutor = ForkJoinPool.commonPool();
}
