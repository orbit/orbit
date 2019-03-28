/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.cluster

import cloud.orbit.core.hosting.AddressableDirectory
import cloud.orbit.runtime.cluster.local.LocalAddressableDirectory

/**
 * Specifies the major networking components for clustering.
 */
data class ClusterConfig(
    /**
     * The [AddressableDirectory] to be used.
     */
    val addressableDirectory: Class<out AddressableDirectory> = LocalAddressableDirectory::class.java
)