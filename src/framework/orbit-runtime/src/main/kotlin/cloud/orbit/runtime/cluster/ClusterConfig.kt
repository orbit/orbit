/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.cluster

import cloud.orbit.runtime.hosting.AddressableDirectory
import cloud.orbit.runtime.hosting.DefaultAddressableDirectory

/**
 * Specifies the major networking components for clustering.
 */
data class ClusterConfig(
    /**
     * The [AddressableDirectory] to be used.
     */
    val addressableDirectory: Class<out AddressableDirectory> = DefaultAddressableDirectory::class.java
)