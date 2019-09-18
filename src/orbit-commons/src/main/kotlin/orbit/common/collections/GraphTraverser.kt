/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GraphTraverser<T>(val getChildren: suspend (T) -> List<T>)  {
    fun traverse(initial: T): Flow<ParentChild<T>> = flow {
        var row = listOf(ParentChild(null, initial))

        do {
            row.forEach { emit(it) }
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
