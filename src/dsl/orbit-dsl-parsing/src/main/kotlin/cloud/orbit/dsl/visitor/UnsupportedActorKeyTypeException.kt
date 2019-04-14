/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

class UnsupportedActorKeyTypeException(keyType: String, val line: Int, val column: Int) :
    RuntimeException("unsupported actor key type '$keyType': must be string, int32, int64, or guid")
