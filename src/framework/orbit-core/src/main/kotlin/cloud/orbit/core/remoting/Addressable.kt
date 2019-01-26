/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

import cloud.orbit.core.annotation.NonConcrete

/**
 * Marker interface that determines an interface is addressable remotely.
 */
@NonConcrete
interface Addressable

typealias AddressableClass = Class<out Addressable>