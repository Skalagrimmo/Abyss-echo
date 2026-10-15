package com.example.engine

import com.example.model.*
import java.util.ArrayDeque
import kotlin.random.Random

data class DungeonFloor(
    val floorNumber: Int,
    val sectorName: String,
    val width: Int,
    val height: Int,
    val tiles: Array<Array<Tile>>,
    val enemies: MutableList<Enemy>,
    val playerStart: GridPos,
    val exitPos: GridPos,
    var pendingDilemma: MoralDilemma? = null
)

object DungeonGenerator {

    private enum class RoomType {
        STANDARD_CHAMBER,
        PILLARED_SANCTUARY,
        FLOODED_CISTERN,
        SMELTER_TRENCH,
        CRUCIBLE_ARENA
    }

    private data class Room(
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val type: RoomType
    ) {
        val centerX: Int get() = x + w / 2
        val centerY: Int get() = y + h / 2

        fun intersects(other: Room, padding: Int = 1): Boolean {
            return x - padding <= other.x + other.w && x + w + padding >= other.x &&
                    y - padding <= other.y + other.h && y + h + padding >= other.y
        }
    }

    fun generateFloor(floorNumber: Int, seed: Long = System.currentTimeMillis()): DungeonFloor {
        val random = Random(seed)
        val width = 22
        val height = 22

        val sectorName = when (floorNumber) {
            1 -> "Рівень 1: Затоплені Катакомби"
            2 -> "Рівень 2: Шлакові Штольні Мануфактури"
            3 -> "Рівень 3: Хтонічне Горнило Бога Фабрики"
            else -> "Рівень $floorNumber: Безодня Нескінченності"
        }

        // 1. Initialize world grid with solid walls
        val tiles = Array(width) { x ->
            Array(height) { y ->
                val isIron = (floorNumber >= 2 && random.nextFloat() < 0.25f)
                Tile(
                    pos = GridPos(x, y),
                    type = if (isIron) TileType.IRON_WALL else TileType.WALL
                )
            }
        }

        // 2. Generate thematic rooms with distinct 16-bit architectural layouts
        val rooms = mutableListOf<Room>()
        val targetRooms = 5 + random.nextInt(3)
        var attempts = 0

        while (rooms.size < targetRooms && attempts < 120) {
            attempts++
            val rw = 4 + random.nextInt(4)
            val rh = 4 + random.nextInt(4)
            val rx = 1 + random.nextInt(width - rw - 2)
            val ry = 1 + random.nextInt(height - rh - 2)

            val roomType = when {
                floorNumber == 3 && rooms.isEmpty() -> RoomType.CRUCIBLE_ARENA
                random.nextFloat() < 0.25f -> RoomType.PILLARED_SANCTUARY
                random.nextFloat() < 0.45f -> RoomType.FLOODED_CISTERN
                random.nextFloat() < 0.65f -> RoomType.SMELTER_TRENCH
                else -> RoomType.STANDARD_CHAMBER
            }

            val candidate = Room(rx, ry, rw, rh, roomType)
            if (rooms.none { it.intersects(candidate, padding = 1) }) {
                rooms.add(candidate)
                carveRoomLayout(tiles, candidate, floorNumber, random)
            }
        }

        // Fallback guarantee: at least 3 rooms
        if (rooms.size < 2) {
            val r1 = Room(2, 2, 6, 6, RoomType.STANDARD_CHAMBER)
            val r2 = Room(12, 12, 7, 7, RoomType.CRUCIBLE_ARENA)
            rooms.add(r1)
            rooms.add(r2)
            carveRoomLayout(tiles, r1, floorNumber, random)
            carveRoomLayout(tiles, r2, floorNumber, random)
        }

        // 3. Connect rooms with corridors with 16-bit archways and natural turns
        for (i in 0 until rooms.size - 1) {
            val r1 = rooms[i]
            val r2 = rooms[i + 1]

            var cx = r1.centerX
            var cy = r1.centerY
            val tx = r2.centerX
            val ty = r2.centerY

            // Carve L-shaped corridor with occasional 2-wide sections
            while (cx != tx) {
                carveCorridorTile(tiles, cx, cy, floorNumber, random)
                cx += if (tx > cx) 1 else -1
            }
            while (cy != ty) {
                carveCorridorTile(tiles, cx, cy, floorNumber, random)
                cy += if (ty > cy) 1 else -1
            }
        }

        // 4. Ensure path connectivity via BFS flood-fill
        val startRoom = rooms.first()
        val exitRoom = rooms.last()
        val playerStart = GridPos(startRoom.centerX, startRoom.centerY)
        val exitPos = GridPos(exitRoom.centerX, exitRoom.centerY)

        ensureConnectivity(tiles, playerStart, exitPos, floorNumber, random)

        // Set elevator exit tile
        tiles[exitPos.x][exitPos.y].type = TileType.ELEVATOR_EXIT

        // 5. Compute 16-bit autotiling bitmasks & perspectives
        compute16BitAutotiling(tiles, width, height, random)

        // 6. Place interactive shrines, altars, and relic chests in distinct rooms
        if (rooms.size > 2) {
            val altarRoom = rooms[1]
            val altarPos = GridPos(altarRoom.centerX, altarRoom.centerY)
            if (altarPos != playerStart && altarPos != exitPos) {
                tiles[altarPos.x][altarPos.y].type = if (floorNumber % 2 == 1) {
                    TileType.CHTHONIC_ALTAR
                } else {
                    TileType.SMELTER_FORGE
                }
            }
        }
        if (rooms.size > 3) {
            val chestRoom = rooms[2]
            val chestPos = GridPos(chestRoom.x + 1, chestRoom.y + 1)
            if (chestPos != playerStart && chestPos != exitPos && tiles[chestPos.x][chestPos.y].isWalkable) {
                tiles[chestPos.x][chestPos.y].type = TileType.RELIC_CHEST
            }
        }

        // 7. Spawn enemies in outer rooms
        val enemies = mutableListOf<Enemy>()
        var enemyIdCounter = 1

        for (i in 1 until rooms.size) {
            val room = rooms[i]
            val isFinalRoom = (i == rooms.size - 1)
            val enemyCount = if (isFinalRoom && floorNumber == 3) 1 else 1 + random.nextInt(2)

            for (k in 0 until enemyCount) {
                val ex = (room.x + 1 + random.nextInt(maxOf(1, room.w - 2))).coerceIn(1, width - 2)
                val ey = (room.y + 1 + random.nextInt(maxOf(1, room.h - 2))).coerceIn(1, height - 2)
                val pos = GridPos(ex, ey)

                if (pos != playerStart && pos != exitPos && tiles[ex][ey].isWalkable) {
                    val arch = when {
                        floorNumber == 3 && isFinalRoom -> EnemyArchetype.CRUCIBLE_TITAN
                        floorNumber == 1 -> if (random.nextBoolean()) EnemyArchetype.CRAWLING_SPIDER else EnemyArchetype.CHTHONIC_CULTIST
                        floorNumber == 2 -> if (random.nextBoolean()) EnemyArchetype.SLAG_HOUND else EnemyArchetype.IRON_AUTOMATON
                        else -> EnemyArchetype.values().filter { it != EnemyArchetype.CRUCIBLE_TITAN }.random(random)
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

        // Dilemma trigger
        val dilemma = when (floorNumber) {
            1 -> DilemmaCatalog.dilemmas[0]
            2 -> DilemmaCatalog.dilemmas[1]
            3 -> DilemmaCatalog.dilemmas[2]
            else -> DilemmaCatalog.dilemmas.random(random)
        }

        return DungeonFloor(
            floorNumber = floorNumber,
            sectorName = sectorName,
            width = width,
            height = height,
            tiles = tiles,
            enemies = enemies,
            playerStart = playerStart,
            exitPos = exitPos,
            pendingDilemma = dilemma
        )
    }

    private fun carveRoomLayout(
        tiles: Array<Array<Tile>>,
        room: Room,
        floorNumber: Int,
        random: Random
    ) {
        for (x in room.x until room.x + room.w) {
            for (y in room.y until room.y + room.h) {
                tiles[x][y].type = TileType.FLOOR
                tiles[x][y].floorVariation = random.nextInt(4)
            }
        }

        when (room.type) {
            RoomType.PILLARED_SANCTUARY -> {
                // Symmetrical 16-bit pillars in corners
                if (room.w >= 5 && room.h >= 5) {
                    val p1X = room.x + 1
                    val p1Y = room.y + 1
                    val p2X = room.x + room.w - 2
                    val p2Y = room.y + room.h - 2

                    tiles[p1X][p1Y].type = TileType.IRON_WALL
                    tiles[p1X][p1Y].isPillar = true

                    tiles[p2X][p1Y].type = TileType.IRON_WALL
                    tiles[p2X][p1Y].isPillar = true

                    tiles[p1X][p2Y].type = TileType.IRON_WALL
                    tiles[p1X][p2Y].isPillar = true

                    tiles[p2X][p2Y].type = TileType.IRON_WALL
                    tiles[p2X][p2Y].isPillar = true
                }
            }
            RoomType.FLOODED_CISTERN -> {
                // Central canal/pool with bridge
                val poolType = if (floorNumber == 1) TileType.WATER_PUDDLE else TileType.TOXIC_MIRE
                val midX = room.centerX
                val midY = room.centerY
                tiles[midX][midY].type = poolType
                if (room.w > 4) {
                    tiles[midX - 1][midY].type = poolType
                    tiles[midX + 1][midY].type = poolType
                }
            }
            RoomType.SMELTER_TRENCH -> {
                // Molten slag trench
                val poolType = if (random.nextBoolean()) TileType.EMBER_HEARTH else TileType.OIL_SLICK
                val midY = room.centerY
                for (x in room.x + 1 until room.x + room.w - 1) {
                    if (random.nextFloat() < 0.7f) {
                        tiles[x][midY].type = poolType
                    }
                }
            }
            RoomType.CRUCIBLE_ARENA -> {
                // Central grate and surrounding embers
                tiles[room.centerX][room.centerY].type = TileType.RUST_GRATE
                if (room.w >= 6 && room.h >= 6) {
                    tiles[room.centerX - 1][room.centerY].type = TileType.RUST_GRATE
                    tiles[room.centerX + 1][room.centerY].type = TileType.RUST_GRATE
                    tiles[room.centerX][room.centerY - 1].type = TileType.RUST_GRATE
                    tiles[room.centerX][room.centerY + 1].type = TileType.RUST_GRATE
                }
            }
            RoomType.STANDARD_CHAMBER -> {
                // Subtle rust grates
                if (random.nextFloat() < 0.4f) {
                    tiles[room.centerX][room.centerY].type = TileType.RUST_GRATE
                }
            }
        }

        // Scatter ambient resources on floor
        if (random.nextFloat() < 0.55f) {
            val lx = room.x + random.nextInt(room.w)
            val ly = room.y + random.nextInt(room.h)
            if (tiles[lx][ly].isWalkable) {
                tiles[lx][ly].scrapLoot = 3 + random.nextInt(7)
                tiles[lx][ly].biomassLoot = 2 + random.nextInt(6)
            }
        }
    }

    private fun carveCorridorTile(
        tiles: Array<Array<Tile>>,
        x: Int,
        y: Int,
        floorNumber: Int,
        random: Random
    ) {
        val w = tiles.size
        val h = tiles[0].size
        if (x in 1 until w - 1 && y in 1 until h - 1) {
            tiles[x][y].type = if (random.nextFloat() < 0.15f) TileType.RUST_GRATE else TileType.FLOOR
            tiles[x][y].floorVariation = random.nextInt(4)
        }
    }

    private fun ensureConnectivity(
        tiles: Array<Array<Tile>>,
        start: GridPos,
        target: GridPos,
        floorNumber: Int,
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
            // Tunnel direct connecting corridor
            var cx = start.x
            var cy = start.y
            while (cx != target.x) {
                tiles[cx][cy].type = TileType.FLOOR
                cx += if (target.x > cx) 1 else -1
            }
            while (cy != target.y) {
                tiles[cx][cy].type = TileType.FLOOR
                cy += if (target.y > cy) 1 else -1
            }
        }
    }

    private fun compute16BitAutotiling(
        tiles: Array<Array<Tile>>,
        width: Int,
        height: Int,
        random: Random
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                val tile = tiles[x][y]
                if (!tile.isWalkable) {
                    // Check if South neighbor is walkable floor -> Wall Front Face!
                    val southWalkable = (y + 1 < height) && tiles[x][y + 1].isWalkable
                    tile.wallFrontFace = southWalkable

                    // Check top edge
                    val northWalkable = (y - 1 >= 0) && tiles[x][y - 1].isWalkable
                    tile.wallTopEdge = northWalkable

                    // Left & Right edges
                    tile.wallLeftEdge = (x - 1 >= 0) && tiles[x - 1][y].isWalkable
                    tile.wallRightEdge = (x + 1 < width) && tiles[x + 1][y].isWalkable

                    // Wall torch placement: on front facades with 25% chance
                    if (tile.wallFrontFace && x % 3 == 1 && random.nextFloat() < 0.45f) {
                        tile.hasWallTorch = true
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

        // Bounded clear around player instead of entire map
        val minX = (center.x - radius - 2).coerceAtLeast(0)
        val maxX = (center.x + radius + 2).coerceAtMost(w - 1)
        val minY = (center.y - radius - 2).coerceAtLeast(0)
        val maxY = (center.y + radius + 2).coerceAtMost(h - 1)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                tiles[x][y].isVisible = false
            }
        }

        // Raycasting using precomputed trig deltas
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
