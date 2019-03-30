/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.hosting

import cloud.orbit.core.remoting.Addressable

/**
 * The execution strategy to be used for this [Addressable].
 */
enum class ExecutionStrategy {
    /**
     * Safe Execution Mode
     * Calls will run serially until completed. No interleaving and no parallelism.
     */
    SAFE
}