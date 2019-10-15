/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.pipeline

import orbit.shared.net.Message

class PipelineException(val lastMsgState: Message, val reason: Throwable) : Throwable(cause = reason)