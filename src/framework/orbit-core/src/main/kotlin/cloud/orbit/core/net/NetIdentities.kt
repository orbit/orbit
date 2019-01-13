/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.net

/**
 * The name of an Orbit cluster.
 *
 * This value allows Orbit nodes to talk to one another.
 */
data class ClusterName(val value: String)

/**
 * The identity of an Orbit node.
 *
 * This value must be unique within a cluster.
 */
data class NodeIdentity(val value: String)