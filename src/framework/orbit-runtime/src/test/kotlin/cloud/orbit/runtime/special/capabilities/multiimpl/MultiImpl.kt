/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.capabilities.multiimpl

import cloud.orbit.core.remoting.Addressable

interface MultiImpl : Addressable

class MultiImpl1 : MultiImpl
class MultiImpl2 : MultiImpl