/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.mesh

import orbit.common.exception.OrbitException

class LeaseExpiredException(nodeId: NodeId): OrbitException("Lease expired for node '$nodeId'")
class InvalidChallengeException(nodeId: NodeId, challengeToken: ChallengeToken): OrbitException("Invalid challenge for node '$nodeId'")
