package com.example.model

data class Tile(
    val pos: GridPos,
    var type: TileType,
    var element: TileElement = TileElement.NONE,
    var elementTurnsRemaining: Int = 0,
    var isDiscovered: Boolean = false,
    var isVisible: Boolean = false,
    var hasBloodSplatter: Boolean = false,
    var scrapLoot: Int = 0,
    var biomassLoot: Int = 0
) {
    val isWalkable: Boolean
        get() = type.isWalkable

    val isTransparent: Boolean
        get() = type.isTransparent
}
