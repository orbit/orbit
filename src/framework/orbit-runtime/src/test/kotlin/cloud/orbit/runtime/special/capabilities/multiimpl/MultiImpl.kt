/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.capabilities.multiimpl

import cloud.orbit.core.remoting.Addressable

interface MultiImpl : Addressable

@Suppress("UNUSED")
class MultiImpl1 : MultiImpl

@Suppress("UNUSED")
class MultiImpl2 : MultiImpl