package com.example.engine

import com.example.model.*
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
    private data class Room(val x: Int, val y: Int, val w: Int, val h: Int) {
        val centerX: Int get() = x + w / 2
        val centerY: Int get() = y + h / 2

        fun intersects(other: Room): Boolean {
            return x <= other.x + other.w + 1 && x + w + 1 >= other.x &&
                    y <= other.y + other.h + 1 && y + h + 1 >= other.y
        }
    }

    fun generateFloor(floorNumber: Int, seed: Long = System.currentTimeMillis()): DungeonFloor {
        val random = Random(seed)
        val width = 20
        val height = 20

        val sectorName = when (floorNumber) {
            1 -> "Рівень 1: Затоплені Катакомби"
            2 -> "Рівень 2: Шлакові Штольні Мануфактури"
            3 -> "Рівень 3: Хтонічне Горнило Бога Фабрики"
            else -> "Рівень $floorNumber: Безодня Нескінченності"
        }

        // Initialize all tiles as WALL
        val tiles = Array(width) { x ->
            Array(height) { y ->
                Tile(
                    pos = GridPos(x, y),
                    type = if (random.nextFloat() < 0.2f) TileType.IRON_WALL else TileType.WALL
                )
            }
        }

        // Carve rooms using BSP-like partitioning
        val rooms = mutableListOf<Room>()
        val targetRooms = 5 + random.nextInt(3)

        var attempts = 0
        while (rooms.size < targetRooms && attempts < 100) {
            attempts++
            val rw = 3 + random.nextInt(4)
            val rh = 3 + random.nextInt(4)
            val rx = 1 + random.nextInt(width - rw - 2)
            val ry = 1 + random.nextInt(height - rh - 2)
            val candidate = Room(rx, ry, rw, rh)

            if (rooms.none { it.intersects(candidate) }) {
                rooms.add(candidate)
                // Carve room floor
                for (x in rx until rx + rw) {
                    for (y in ry until ry + rh) {
                        tiles[x][y].type = when {
                            random.nextFloat() < 0.15f -> TileType.RUST_GRATE
                            else -> TileType.FLOOR
                        }
                    }
                }
            }
        }

        // Fallback if too few rooms
        if (rooms.size < 2) {
            rooms.add(Room(2, 2, 5, 5))
            rooms.add(Room(12, 12, 5, 5))
            for (r in rooms) {
                for (x in r.x until r.x + r.w) {
                    for (y in r.y until r.y + r.h) {
                        tiles[x][y].type = TileType.FLOOR
                    }
                }
            }
        }

        // Connect rooms with corridors
        for (i in 0 until rooms.size - 1) {
            val r1 = rooms[i]
            val r2 = rooms[i + 1]

            var cx = r1.centerX
            var cy = r1.centerY
            val targetX = r2.centerX
            val targetY = r2.centerY

            while (cx != targetX) {
                tiles[cx][cy].type = TileType.FLOOR
                cx += if (targetX > cx) 1 else -1
            }
            while (cy != targetY) {
                tiles[cx][cy].type = TileType.FLOOR
                cy += if (targetY > cy) 1 else -1
            }
        }

        // Place interactive elements & environmental hazards
        for (r in rooms) {
            // Chance of environmental pool
            if (random.nextFloat() < 0.6f) {
                val poolX = r.x + 1 + random.nextInt(maxOf(1, r.w - 2))
                val poolY = r.y + 1 + random.nextInt(maxOf(1, r.h - 2))
                val poolType = when (floorNumber) {
                    1 -> if (random.nextBoolean()) TileType.WATER_PUDDLE else TileType.TOXIC_MIRE
                    2 -> if (random.nextBoolean()) TileType.OIL_SLICK else TileType.EMBER_HEARTH
                    else -> listOf(TileType.TOXIC_MIRE, TileType.OIL_SLICK, TileType.EMBER_HEARTH).random(random)
                }
                tiles[poolX][poolY].type = poolType
            }

            // Scatter scrap and biomass loot on floor
            if (random.nextFloat() < 0.5f) {
                val lx = r.x + random.nextInt(r.w)
                val ly = r.y + random.nextInt(r.h)
                if (tiles[lx][ly].isWalkable) {
                    tiles[lx][ly].scrapLoot = 4 + random.nextInt(8)
                    tiles[lx][ly].biomassLoot = 3 + random.nextInt(6)
                }
            }
        }

        // Start room & exit room
        val startRoom = rooms.first()
        val exitRoom = rooms.last()
        val playerStart = GridPos(startRoom.centerX, startRoom.centerY)
        val exitPos = GridPos(exitRoom.centerX, exitRoom.centerY)
        tiles[exitPos.x][exitPos.y].type = TileType.ELEVATOR_EXIT

        // Place special interactables in middle rooms
        if (rooms.size > 2) {
            val midRoom1 = rooms[1]
            val altarPos = GridPos(midRoom1.centerX, midRoom1.centerY)
            if (altarPos != playerStart && altarPos != exitPos) {
                tiles[altarPos.x][altarPos.y].type = if (floorNumber % 2 == 1) TileType.CHTHONIC_ALTAR else TileType.SMELTER_FORGE
            }
        }
        if (rooms.size > 3) {
            val midRoom2 = rooms[2]
            val chestPos = GridPos(midRoom2.x + 1, midRoom2.y + 1)
            if (chestPos != playerStart && chestPos != exitPos) {
                tiles[chestPos.x][chestPos.y].type = TileType.RELIC_CHEST
            }
        }

        // Spawn enemies
        val enemies = mutableListOf<Enemy>()
        var enemyIdCounter = 1

        for (i in 1 until rooms.size) {
            val room = rooms[i]
            val count = if (i == rooms.size - 1 && floorNumber == 3) 1 else 1 + random.nextInt(2)

            for (k in 0 until count) {
                val ex = room.x + random.nextInt(room.w)
                val ey = room.y + random.nextInt(room.h)
                val pos = GridPos(ex, ey)

                if (pos != playerStart && pos != exitPos && tiles[ex][ey].isWalkable) {
                    val arch = when {
                        floorNumber == 3 && i == rooms.size - 1 -> EnemyArchetype.CRUCIBLE_TITAN
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

        // Dilemma trigger on floor 1, 2, and 3
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

    fun calculateFov(tiles: Array<Array<Tile>>, center: GridPos, radius: Int) {
        val w = tiles.size
        val h = tiles[0].size

        // Reset visibility
        for (x in 0 until w) {
            for (y in 0 until h) {
                tiles[x][y].isVisible = false
            }
        }

        // Simple raycasting FOV
        val stepCount = 90
        for (i in 0 until stepCount) {
            val angle = i * (2.0 * Math.PI / stepCount)
            val dx = Math.cos(angle)
            val dy = Math.sin(angle)

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
