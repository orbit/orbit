/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import java.lang.RuntimeException

class OrbitDslTypeCheckingException(val typeErrors: Collection<OrbitDslTypeError>) : RuntimeException()
