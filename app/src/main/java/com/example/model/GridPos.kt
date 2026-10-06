package com.example.model

import kotlin.math.abs
import kotlin.math.max

data class GridPos(val x: Int, val y: Int) {
    fun manhattanDistance(other: GridPos): Int {
        return abs(x - other.x) + abs(y - other.y)
    }

    fun chebyshevDistance(other: GridPos): Int {
        return max(abs(x - other.x), abs(y - other.y))
    }

    fun adjacentNeighbors(includeDiagonals: Boolean = false): List<GridPos> {
        val list = mutableListOf(
            GridPos(x + 1, y),
            GridPos(x - 1, y),
            GridPos(x, y + 1),
            GridPos(x, y - 1)
        )
        if (includeDiagonals) {
            list.add(GridPos(x + 1, y + 1))
            list.add(GridPos(x - 1, y + 1))
            list.add(GridPos(x + 1, y - 1))
            list.add(GridPos(x - 1, y - 1))
        }
        return list
    }

    fun directionTo(target: GridPos): GridPos {
        val dx = (target.x - x).coerceIn(-1, 1)
        val dy = (target.y - y).coerceIn(-1, 1)
        return GridPos(dx, dy)
    }
}
