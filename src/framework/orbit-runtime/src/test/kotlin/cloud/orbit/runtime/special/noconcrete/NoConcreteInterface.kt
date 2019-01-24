/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.special.noconcrete

import cloud.orbit.core.annotation.NonConcrete
import cloud.orbit.core.remoting.Addressable

@NonConcrete
interface NoConcreteInterface : Addressable

class NoConcreteClass : NoConcreteInterface