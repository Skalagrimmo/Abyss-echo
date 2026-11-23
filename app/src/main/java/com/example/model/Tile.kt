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
    var biomassLoot: Int = 0,
    // 16-bit autotiling and decorative styling
    var wallFrontFace: Boolean = false, // Has walkable floor directly to the south
    var wallTopEdge: Boolean = false,   // Has floor or void to the north
    var wallLeftEdge: Boolean = false,  // Has floor to the west
    var wallRightEdge: Boolean = false, // Has floor to the east
    var hasWallTorch: Boolean = false,  // Decorative wall sconce with flame
    var floorVariation: Int = 0,        // 0: smooth, 1: cracked, 2: runic, 3: metal trim
    var isPillar: Boolean = false,      // Isolated column/pillar (wallMask == 0)
    // Biomechanical tileset autotiling data
    var biome: TilesetBiome = TilesetBiome.FLESH,
    var wallMask: Int = 0,              // 4-bit cardinal mask (N=1, E=2, S=4, W=8) for adjacent walls
    var conduitMask: Int = 0,           // 4-bit cardinal mask (N=1, E=2, S=4, W=8) for connected pipes/veins
    var decal: BiomechDecal = BiomechDecal.NONE,
    var hasWallPipe: Boolean = false,
    var hasWallTendril: Boolean = false
) {
    val isWalkable: Boolean
        get() = type.isWalkable

    val isTransparent: Boolean
        get() = type.isTransparent
}
