package com.example

import com.example.engine.DungeonFloor
import com.example.engine.DungeonGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DungeonGeneratorDeterminismTest {
    @Test
    fun sameSeedProducesSameFloor() {
        val first = DungeonGenerator.generateFloor(1, seed = 42L)
        val second = DungeonGenerator.generateFloor(1, seed = 42L)
        assertEquals(first.sectorName, second.sectorName)
        assertEquals(first.playerStart, second.playerStart)
        assertEquals(first.exitPos, second.exitPos)
        assertEquals(tileSignature(first), tileSignature(second))
        assertEquals(enemySignature(first), enemySignature(second))
    }

    @Test
    fun differentSeedsCanProduceDifferentFloors() {
        val first = DungeonGenerator.generateFloor(1, seed = 42L)
        val second = DungeonGenerator.generateFloor(1, seed = 43L)
        assertNotEquals(tileSignature(first), tileSignature(second))
    }

    private fun tileSignature(floor: DungeonFloor): String =
        floor.tiles.joinToString("|") { column ->
            column.joinToString(",") { t ->
                "${t.pos.x}:${t.pos.y}:${t.type}:${t.scrapLoot}:${t.biomassLoot}"
            }
        }

    private fun enemySignature(floor: DungeonFloor): String =
        floor.enemies.joinToString("|") { enemy ->
            "${enemy.id}:${enemy.archetype}:${enemy.pos.x}:${enemy.pos.y}"
        }
}