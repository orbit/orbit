/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.remoting

import cloud.orbit.core.remoting.AddressableClass

data class RemoteInterfaceDefinition(
    val interfaceClass: AddressableClass,
    val interfaceName: String
)