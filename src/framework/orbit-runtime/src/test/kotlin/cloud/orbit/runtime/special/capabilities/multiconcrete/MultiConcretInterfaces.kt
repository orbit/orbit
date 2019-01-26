/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.capabilities.multiconcrete

import cloud.orbit.core.remoting.Addressable

interface Concrete1 : Addressable
interface Concrete2 : Addressable
class ConcreteClass : Concrete1, Concrete2