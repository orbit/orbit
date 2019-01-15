/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.remoting

/**
 * Marker interface that determines an interface is addressable remotely.
 */
interface Addressable

typealias AddressableClass = Class<out Addressable>