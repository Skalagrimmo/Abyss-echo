package com.example.engine

import com.example.model.*
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.random.Random

data class DungeonFloor(
    val floorNumber: Int,
    val sectorName: String,
    val biome: TilesetBiome = TilesetBiome.FLESH,
    val width: Int,
    val height: Int,
    val tiles: Array<Array<Tile>>,
    val enemies: MutableList<Enemy>,
    val props: MutableList<TacticalProp> = mutableListOf(),
    val playerStart: GridPos,
    val exitPos: GridPos,
    var pendingDilemma: MoralDilemma? = null
)

object DungeonGenerator {

    private data class Room(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val isStart: Boolean = false
    ) {
        val centerX: Int get() = x + w / 2
        val centerY: Int get() = y + h / 2
    }

    private class BspNode(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        var left: BspNode? = null,
        var right: BspNode? = null,
        var room: Room? = null
    ) {
        val isLeaf: Boolean get() = left == null && right == null

        fun anyRoom(): Room? = room ?: left?.anyRoom() ?: right?.anyRoom()
    }

    fun biomeForFloor(floorNumber: Int): TilesetBiome = when (floorNumber) {
        1 -> TilesetBiome.FLESH
        2 -> TilesetBiome.STEEL
        else -> TilesetBiome.CRUCIBLE
    }

    fun generateFloor(floorNumber: Int, seed: Long = System.currentTimeMillis()): DungeonFloor {
        val random = Random(seed)
        val width = 26
        val height = 26
        val biome = biomeForFloor(floorNumber)

        val sectorName = when (floorNumber) {
            1 -> "Ярус 1: Некро-Катакомби Плоті"
            2 -> "Ярус 2: Шлакова Мануфактура Сталі"
            3 -> "Ярус 3: Хтонічне Горнило Безодні"
            else -> "Ярус $floorNumber: Бездонний Реактор"
        }

        // 1. Initialize solid biomechanical shell walls
        val tiles = Array(width) { x ->
            Array(height) { y ->
                val defaultWall = when (biome) {
                    TilesetBiome.FLESH -> if (random.nextFloat() < 0.22f) TileType.IRON_WALL else TileType.WALL
                    TilesetBiome.STEEL -> if (random.nextFloat() < 0.78f) TileType.IRON_WALL else TileType.WALL
                    TilesetBiome.CRUCIBLE -> if (random.nextFloat() < 0.55f) TileType.IRON_WALL else TileType.WALL
                }
                Tile(
                    pos = GridPos(x, y),
                    type = defaultWall,
                    biome = biome
                )
            }
        }

        // 2. Partition floor space with Binary Space Partitioning (BSP)
        val root = BspNode(1, 1, width - 2, height - 2)
        splitBsp(root, depth = 0, maxDepth = 3, minLeafSize = 7, random = random)

        // 3. Carve rooms inside BSP leaves and stamp biomechanical architectural templates
        val rooms = mutableListOf<Room>()
        populateBspRooms(root, tiles, biome, rooms, random)

        // Guarantee minimum chambers if RNG split aggressively
        if (rooms.size < 2) {
            val r1 = Room(2, 2, 6, 6, isStart = true)
            val r2 = Room(width - 9, height - 9, 7, 7)
            rooms.add(r1)
            rooms.add(r2)
            carveRoomWithStamp(tiles, r1, biome, applyStamp = false, random = random)
            carveRoomWithStamp(tiles, r2, biome, applyStamp = true, random = random)
        }

        // 4. Connect BSP siblings recursively via biomechanical conduit corridors
        connectBspSubtrees(root, tiles, biome, random)

        // Add 1-2 extra flanking loop corridors between nearest rooms for tactical depth
        if (rooms.size >= 4) {
            val extraConnections = minOf(2, rooms.size / 2)
            for (c in 0 until extraConnections) {
                val rA = rooms[c]
                val rB = rooms[(c + 2) % rooms.size]
                carveBiomechanicalCorridor(tiles, rA.centerX, rA.centerY, rB.centerX, rB.centerY, biome, random)
            }
        }

        // Pick farthest room pair (start = first, exit = farthest from start)
        val startRoom = rooms.first()
        val playerStart = GridPos(startRoom.centerX, startRoom.centerY)
        val exitRoom = rooms.maxByOrNull {
            abs(it.centerX - playerStart.x) + abs(it.centerY - playerStart.y)
        } ?: rooms.last()
        val exitPos = GridPos(exitRoom.centerX, exitRoom.centerY)

        // 5. Verify full BFS connectivity from playerStart to all rooms and exit
        for (room in rooms) {
            ensureConnectivity(tiles, playerStart, GridPos(room.centerX, room.centerY), biome, random)
        }
        ensureConnectivity(tiles, playerStart, exitPos, biome, random)

        // Guarantee safe start & clear elevator exit
        tiles[playerStart.x][playerStart.y].type = TileType.FLOOR
        tiles[exitPos.x][exitPos.y].type = TileType.ELEVATOR_EXIT

        // 6. Compute 4-bit cardinal autotiling masks, wall facades, and conduit links
        compute16BitAutotiling(tiles, width, height, biome, random)

        // 7. Place interactive altars, forges, and relic chests in chambers
        val midRooms = rooms.filter { it != startRoom && it != exitRoom }
        if (midRooms.isNotEmpty()) {
            val altarRoom = midRooms[0]
            val altarPos = findNearestWalkableCell(tiles, GridPos(altarRoom.centerX, altarRoom.centerY), playerStart, exitPos)
            if (altarPos != null) {
                tiles[altarPos.x][altarPos.y].type = when (biome) {
                    TilesetBiome.FLESH -> TileType.CHTHONIC_ALTAR
                    TilesetBiome.STEEL -> TileType.SMELTER_FORGE
                    TilesetBiome.CRUCIBLE -> if (random.nextBoolean()) TileType.CHTHONIC_ALTAR else TileType.SMELTER_FORGE
                }
            }
        }
        if (midRooms.size > 1) {
            val chestRoom = midRooms[1]
            val candidate = GridPos(chestRoom.x + 1, chestRoom.y + 1)
            if (candidate != playerStart && candidate != exitPos && tiles[candidate.x][candidate.y].isWalkable) {
                tiles[candidate.x][candidate.y].type = TileType.RELIC_CHEST
            }
        }

        // Recompute wall masks in case altar/chest placement changed walkability
        compute16BitAutotiling(tiles, width, height, biome, random)

        // 8. Spawn biome-appropriate enemies in non-start rooms
        val enemies = mutableListOf<Enemy>()
        var enemyIdCounter = 1

        for (room in rooms) {
            if (room == startRoom) continue
            val isExitChamber = (room == exitRoom)
            val enemyCount = if (isExitChamber && floorNumber >= 3) 1 else 1 + random.nextInt(2)

            for (k in 0 until enemyCount) {
                val ex = (room.x + random.nextInt(room.w)).coerceIn(1, width - 2)
                val ey = (room.y + random.nextInt(room.h)).coerceIn(1, height - 2)
                val pos = GridPos(ex, ey)

                if (pos != playerStart && pos != exitPos &&
                    tiles[ex][ey].isWalkable &&
                    enemies.none { it.pos == pos }
                ) {
                    val arch = when {
                        floorNumber >= 3 && isExitChamber -> EnemyArchetype.CRUCIBLE_TITAN
                        biome == TilesetBiome.FLESH -> if (random.nextBoolean()) EnemyArchetype.CRAWLING_SPIDER else EnemyArchetype.CHTHONIC_CULTIST
                        biome == TilesetBiome.STEEL -> if (random.nextBoolean()) EnemyArchetype.IRON_AUTOMATON else EnemyArchetype.SLAG_HOUND
                        else -> EnemyArchetype.entries.filter { it != EnemyArchetype.CRUCIBLE_TITAN }.random(random)
                    }

                    enemies.add(
                        Enemy(
                            id = "enemy_${floorNumber}_$enemyIdCounter",
                            archetype = arch,
                            pos = pos,
                            currentHp = arch.maxHp,
                            maxHp = arch.maxHp,
                            armor = arch.armor
                        )
                    )
                    enemyIdCounter++
                }
            }
        }

        // 9. Spawn tactical interactive obstacles (explosive barrels, crates, traps)
        val props = mutableListOf<TacticalProp>()
        var propIdCounter = 1

        for (room in rooms) {
            val propCount = 1 + random.nextInt(2)
            for (p in 0 until propCount) {
                val px = (room.x + random.nextInt(room.w)).coerceIn(1, width - 2)
                val py = (room.y + random.nextInt(room.h)).coerceIn(1, height - 2)
                val pPos = GridPos(px, py)

                if (pPos != playerStart && pPos != exitPos &&
                    tiles[px][py].isWalkable &&
                    enemies.none { it.pos == pPos } &&
                    props.none { it.pos == pPos }
                ) {
                    val pType = when (biome) {
                        TilesetBiome.FLESH -> when {
                            random.nextFloat() < 0.4f -> TacticalPropType.EXPLOSIVE_BARREL
                            random.nextFloat() < 0.75f -> TacticalPropType.SPIKE_PLATE
                            else -> TacticalPropType.BARRICADE_CRATE
                        }
                        TilesetBiome.STEEL -> when {
                            random.nextFloat() < 0.45f -> TacticalPropType.EXPLOSIVE_BARREL
                            random.nextFloat() < 0.78f -> TacticalPropType.STEAM_PRESSURE_VENT
                            else -> TacticalPropType.BARRICADE_CRATE
                        }
                        TilesetBiome.CRUCIBLE -> when {
                            random.nextFloat() < 0.55f -> TacticalPropType.EXPLOSIVE_BARREL
                            random.nextFloat() < 0.85f -> TacticalPropType.STEAM_PRESSURE_VENT
                            else -> TacticalPropType.SPIKE_PLATE
                        }
                    }
                    props.add(TacticalProp("prop_${floorNumber}_$propIdCounter", pPos, pType))
                    propIdCounter++
                }
            }
        }

        val dilemma = when (floorNumber) {
            1 -> DilemmaCatalog.dilemmas[0]
            2 -> DilemmaCatalog.dilemmas[1]
            3 -> DilemmaCatalog.dilemmas[2]
            else -> DilemmaCatalog.dilemmas.random(random)
        }

        return DungeonFloor(
            floorNumber = floorNumber,
            sectorName = sectorName,
            biome = biome,
            width = width,
            height = height,
            tiles = tiles,
            enemies = enemies,
            props = props,
            playerStart = playerStart,
            exitPos = exitPos,
            pendingDilemma = dilemma
        )
    }

    private fun splitBsp(
        node: BspNode,
        depth: Int,
        maxDepth: Int,
        minLeafSize: Int,
        random: Random
    ) {
        if (depth >= maxDepth) return

        val canSplitH = node.h >= minLeafSize * 2
        val canSplitV = node.w >= minLeafSize * 2
        if (!canSplitH && !canSplitV) return

        val splitHorizontally = when {
            canSplitH && !canSplitV -> true
            canSplitV && !canSplitH -> false
            node.w > node.h * 1.25f -> false
            node.h > node.w * 1.25f -> true
            else -> random.nextBoolean()
        }

        if (splitHorizontally) {
            val splitAt = random.nextInt(minLeafSize, node.h - minLeafSize + 1)
            node.left = BspNode(node.x, node.y, node.w, splitAt)
            node.right = BspNode(node.x, node.y + splitAt, node.w, node.h - splitAt)
        } else {
            val splitAt = random.nextInt(minLeafSize, node.w - minLeafSize + 1)
            node.left = BspNode(node.x, node.y, splitAt, node.h)
            node.right = BspNode(node.x + splitAt, node.y, node.w - splitAt, node.h)
        }

        node.left?.let { splitBsp(it, depth + 1, maxDepth, minLeafSize, random) }
        node.right?.let { splitBsp(it, depth + 1, maxDepth, minLeafSize, random) }
    }

    private fun populateBspRooms(
        node: BspNode,
        tiles: Array<Array<Tile>>,
        biome: TilesetBiome,
        outRooms: MutableList<Room>,
        random: Random
    ) {
        if (node.isLeaf) {
            val maxW = (node.w - 2).coerceAtLeast(5)
            val maxH = (node.h - 2).coerceAtLeast(5)
            val rw = if (maxW > 5) random.nextInt(5, maxW + 1) else maxW.coerceAtMost(node.w - 1)
            val rh = if (maxH > 5) random.nextInt(5, maxH + 1) else maxH.coerceAtMost(node.h - 1)

            val slackX = (node.w - rw - 1).coerceAtLeast(1)
            val slackY = (node.h - rh - 1).coerceAtLeast(1)
            val rx = node.x + random.nextInt(1, slackX + 1)
            val ry = node.y + random.nextInt(1, slackY + 1)

            val isFirst = outRooms.isEmpty()
            val room = Room(rx, ry, rw, rh, isStart = isFirst)
            node.room = room
            outRooms.add(room)
            carveRoomWithStamp(tiles, room, biome, applyStamp = !isFirst, random = random)
        } else {
            node.left?.let { populateBspRooms(it, tiles, biome, outRooms, random) }
            node.right?.let { populateBspRooms(it, tiles, biome, outRooms, random) }
        }
    }

    private fun carveRoomWithStamp(
        tiles: Array<Array<Tile>>,
        room: Room,
        biome: TilesetBiome,
        applyStamp: Boolean,
        random: Random
    ) {
        for (x in room.x until room.x + room.w) {
            for (y in room.y until room.y + room.h) {
                if (x in 1 until tiles.size - 1 && y in 1 until tiles[0].size - 1) {
                    tiles[x][y].type = TileType.FLOOR
                    tiles[x][y].biome = biome
                    tiles[x][y].floorVariation = random.nextInt(4)
                }
            }
        }

        if (applyStamp) {
            val stamp = BiomechRoomStamps.pickStampFor(biome, room.w, room.h, random)
            if (stamp != null) {
                BiomechRoomStamps.applyStampToRoom(
                    tiles = tiles,
                    roomX = room.x,
                    roomY = room.y,
                    roomW = room.w,
                    roomH = room.h,
                    biome = biome,
                    stamp = stamp,
                    random = random
                )
            }
        } else {
            // Subtle biomechanical trim on start chamber
            tiles[room.centerX][room.centerY].decal = BiomechDecal.RIVET_PLATE
        }

        // Ambient resource drop
        if (random.nextFloat() < 0.6f) {
            val lx = (room.x + random.nextInt(room.w)).coerceIn(1, tiles.size - 2)
            val ly = (room.y + random.nextInt(room.h)).coerceIn(1, tiles[0].size - 2)
            if (tiles[lx][ly].isWalkable) {
                tiles[lx][ly].scrapLoot = 3 + random.nextInt(7)
                tiles[lx][ly].biomassLoot = 2 + random.nextInt(6)
            }
        }
    }

    private fun connectBspSubtrees(
        node: BspNode,
        tiles: Array<Array<Tile>>,
        biome: TilesetBiome,
        random: Random
    ) {
        val leftChild = node.left ?: return
        val rightChild = node.right ?: return

        connectBspSubtrees(leftChild, tiles, biome, random)
        connectBspSubtrees(rightChild, tiles, biome, random)

        val r1 = leftChild.anyRoom()
        val r2 = rightChild.anyRoom()
        if (r1 != null && r2 != null) {
            carveBiomechanicalCorridor(tiles, r1.centerX, r1.centerY, r2.centerX, r2.centerY, biome, random)
        }
    }

    private fun carveBiomechanicalCorridor(
        tiles: Array<Array<Tile>>,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        biome: TilesetBiome,
        random: Random
    ) {
        var cx = x1
        var cy = y1
        val horizontalFirst = random.nextBoolean()
        val carryConduit = random.nextFloat() < 0.72f

        if (horizontalFirst) {
            while (cx != x2) {
                carveCorridorStep(tiles, cx, cy, biome, carryConduit, random)
                cx += if (x2 > cx) 1 else -1
            }
            while (cy != y2) {
                carveCorridorStep(tiles, cx, cy, biome, carryConduit, random)
                cy += if (y2 > cy) 1 else -1
            }
        } else {
            while (cy != y2) {
                carveCorridorStep(tiles, cx, cy, biome, carryConduit, random)
                cy += if (y2 > cy) 1 else -1
            }
            while (cx != x2) {
                carveCorridorStep(tiles, cx, cy, biome, carryConduit, random)
                cx += if (x2 > cx) 1 else -1
            }
        }
        carveCorridorStep(tiles, x2, y2, biome, carryConduit, random)
    }

    private fun carveCorridorStep(
        tiles: Array<Array<Tile>>,
        x: Int,
        y: Int,
        biome: TilesetBiome,
        carryConduit: Boolean,
        random: Random
    ) {
        val w = tiles.size
        val h = tiles[0].size
        if (x !in 1 until w - 1 || y !in 1 until h - 1) return

        val tile = tiles[x][y]
        tile.biome = biome
        if (!tile.isWalkable) {
            tile.type = if (random.nextFloat() < 0.16f) TileType.RUST_GRATE else TileType.FLOOR
            tile.floorVariation = random.nextInt(4)
        }
        if (carryConduit && tile.decal == BiomechDecal.NONE) {
            tile.decal = when (biome) {
                TilesetBiome.FLESH -> BiomechDecal.FLESH_VEINS
                TilesetBiome.STEEL -> BiomechDecal.COPPER_PIPE
                TilesetBiome.CRUCIBLE -> BiomechDecal.RUNIC_SEAM
            }
        }
    }

    private fun findNearestWalkableCell(
        tiles: Array<Array<Tile>>,
        center: GridPos,
        excludeA: GridPos,
        excludeB: GridPos
    ): GridPos? {
        if (center != excludeA && center != excludeB && tiles[center.x][center.y].isWalkable) {
            return center
        }
        for (n in center.adjacentNeighbors(includeDiagonals = true)) {
            if (n.x in 1 until tiles.size - 1 && n.y in 1 until tiles[0].size - 1) {
                if (n != excludeA && n != excludeB && tiles[n.x][n.y].isWalkable) {
                    return n
                }
            }
        }
        return null
    }

    private fun ensureConnectivity(
        tiles: Array<Array<Tile>>,
        start: GridPos,
        target: GridPos,
        biome: TilesetBiome,
        random: Random
    ) {
        val w = tiles.size
        val h = tiles[0].size
        val visited = Array(w) { BooleanArray(h) }
        val queue = ArrayDeque<GridPos>()

        queue.add(start)
        visited[start.x][start.y] = true

        var reached = false
        while (queue.isNotEmpty()) {
            val curr = queue.poll()
            if (curr.x == target.x && curr.y == target.y) {
                reached = true
                break
            }
            for (neighbor in curr.adjacentNeighbors(includeDiagonals = false)) {
                if (neighbor.x in 0 until w && neighbor.y in 0 until h) {
                    if (!visited[neighbor.x][neighbor.y] && tiles[neighbor.x][neighbor.y].isWalkable) {
                        visited[neighbor.x][neighbor.y] = true
                        queue.add(neighbor)
                    }
                }
            }
        }

        if (!reached) {
            carveBiomechanicalCorridor(tiles, start.x, start.y, target.x, target.y, biome, random)
        }
    }

    private fun isStructuralWall(type: TileType): Boolean =
        type == TileType.VOID || type == TileType.WALL || type == TileType.IRON_WALL

    /**
     * Computes 4-bit cardinal autotiling masks (N=1, E=2, S=4, W=8) for walls and floor conduits.
     */
    fun compute16BitAutotiling(
        tiles: Array<Array<Tile>>,
        width: Int,
        height: Int,
        biome: TilesetBiome,
        random: Random
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val tile = tiles[x][y]
                if (isStructuralWall(tile.type)) {
                    val northWall = (y - 1 < 0) || isStructuralWall(tiles[x][y - 1].type)
                    val eastWall = (x + 1 >= width) || isStructuralWall(tiles[x + 1][y].type)
                    val southWall = (y + 1 >= height) || isStructuralWall(tiles[x][y + 1].type)
                    val westWall = (x - 1 < 0) || isStructuralWall(tiles[x - 1][y].type)

                    var mask = 0
                    if (northWall) mask = mask or 1
                    if (eastWall) mask = mask or 2
                    if (southWall) mask = mask or 4
                    if (westWall) mask = mask or 8

                    tile.wallMask = mask
                    tile.wallFrontFace = (mask and 4) == 0 && y + 1 < height
                    tile.wallTopEdge = (mask and 1) == 0 && y - 1 >= 0
                    tile.wallLeftEdge = (mask and 8) == 0 && x - 1 >= 0
                    tile.wallRightEdge = (mask and 2) == 0 && x + 1 < width
                    tile.isPillar = (mask == 0)

                    // Wall facade decorations based on biome & spacing
                    if (tile.wallFrontFace) {
                        if (x % 3 == 1 && random.nextFloat() < 0.42f) {
                            tile.hasWallTorch = true
                        }
                        tile.hasWallPipe = (biome == TilesetBiome.STEEL || biome == TilesetBiome.CRUCIBLE) &&
                                !tile.hasWallTorch && ((x + y) % 2 == 0)
                        tile.hasWallTendril = (biome == TilesetBiome.FLESH || biome == TilesetBiome.CRUCIBLE) &&
                                !tile.hasWallTorch && (random.nextFloat() < 0.35f)
                    } else {
                        tile.hasWallTorch = false
                        tile.hasWallPipe = false
                        tile.hasWallTendril = false
                    }
                } else {
                    tile.wallMask = 0
                    tile.wallFrontFace = false
                    tile.wallTopEdge = false
                    tile.wallLeftEdge = false
                    tile.wallRightEdge = false
                    tile.isPillar = false
                    tile.hasWallTorch = false
                    tile.hasWallPipe = false
                    tile.hasWallTendril = false

                    // 4-bit conduit connection mask for pipes / blood veins / runic seams
                    if (tile.decal == BiomechDecal.FLESH_VEINS ||
                        tile.decal == BiomechDecal.COPPER_PIPE ||
                        tile.decal == BiomechDecal.RUNIC_SEAM
                    ) {
                        var cMask = 0
                        if (y - 1 >= 0 && tiles[x][y - 1].decal == tile.decal) cMask = cMask or 1
                        if (x + 1 < width && tiles[x + 1][y].decal == tile.decal) cMask = cMask or 2
                        if (y + 1 < height && tiles[x][y + 1].decal == tile.decal) cMask = cMask or 4
                        if (x - 1 >= 0 && tiles[x - 1][y].decal == tile.decal) cMask = cMask or 8
                        tile.conduitMask = cMask
                    } else {
                        tile.conduitMask = 0
                    }
                }
            }
        }
    }

    private const val FOV_RAYS_COUNT = 72
    private val fovDx = DoubleArray(FOV_RAYS_COUNT) { i -> Math.cos(i * (2.0 * Math.PI / FOV_RAYS_COUNT)) }
    private val fovDy = DoubleArray(FOV_RAYS_COUNT) { i -> Math.sin(i * (2.0 * Math.PI / FOV_RAYS_COUNT)) }

    fun calculateFov(tiles: Array<Array<Tile>>, center: GridPos, radius: Int) {
        val w = tiles.size
        val h = tiles[0].size

        val minX = (center.x - radius - 2).coerceAtLeast(0)
        val maxX = (center.x + radius + 2).coerceAtMost(w - 1)
        val minY = (center.y - radius - 2).coerceAtLeast(0)
        val maxY = (center.y + radius + 2).coerceAtMost(h - 1)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                tiles[x][y].isVisible = false
            }
        }

        for (i in 0 until FOV_RAYS_COUNT) {
            val dx = fovDx[i]
            val dy = fovDy[i]

            var cx = center.x + 0.5
            var cy = center.y + 0.5

            for (r in 0..radius) {
                val gx = cx.toInt()
                val gy = cy.toInt()
                if (gx in 0 until w && gy in 0 until h) {
                    tiles[gx][gy].isDiscovered = true
                    tiles[gx][gy].isVisible = true
                    if (!tiles[gx][gy].isTransparent) {
                        break
                    }
                }
                cx += dx
                cy += dy
            }
        }
    }
}
