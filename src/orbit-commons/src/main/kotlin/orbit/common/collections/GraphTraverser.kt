/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class GraphTraverser<T>(override val coroutineContext: CoroutineContext, val getChildren: suspend (T) -> List<T>) :
    CoroutineScope {
    fun traverse(initial: T) = produce {
        var row = listOf(ParentChild(null, initial))

        do {
            row.forEach { send(it) }
            row = row.flatMap { node ->
                getChildren(node.child).map { child ->
                    ParentChild(
                        node.child,
                        child
                    )
                }
            }.toList()
        } while (row.isNotEmpty())
    }
}

data class ParentChild<T>(val parent: T?, val child: T)
