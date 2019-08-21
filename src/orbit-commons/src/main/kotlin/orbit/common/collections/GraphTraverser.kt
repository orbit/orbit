/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

class GraphTraverser<T>(val getChildren: (T) -> Sequence<T>) {
    fun traverse(initial: T): Sequence<ParentChild<T>> {
        return sequence() {
            var row = listOf(ParentChild(null, initial))
            do {
                yieldAll(row)
                row = row.asSequence().flatMap { node -> getChildren(node.child).map { child ->
                    ParentChild(
                        node.child,
                        child
                    )
                } }.toList()
            } while (row.isNotEmpty())
        }
    }

    data class ParentChild<T>(val parent: T?, val child: T) {

    }
}
