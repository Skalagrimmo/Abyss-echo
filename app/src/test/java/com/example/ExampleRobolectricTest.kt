package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CombatEngine
import com.example.engine.DungeonGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ехо Безодні", appName)
    }

    @Test
    fun `dungeon generator generates valid walkable path and player start`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 12345L)
        assertNotNull(floor)
        assertEquals(1, floor.floorNumber)
        assertTrue(floor.tiles.isNotEmpty())
        assertTrue(floor.tiles[floor.playerStart.x][floor.playerStart.y].isWalkable)
        assertTrue(floor.tiles[floor.exitPos.x][floor.exitPos.y].type == TileType.ELEVATOR_EXIT)
    }

    @Test
    fun `combat engine executes attack and updates combo chain`() {
        val player = Player(
            pos = GridPos(1, 1),
            currentHp = 50,
            maxHp = 50,
            armor = 2,
            sanity = 50
        )
        val enemy = Enemy(
            id = "test_enemy",
            archetype = EnemyArchetype.CRAWLING_SPIDER,
            pos = GridPos(1, 2),
            currentHp = 18,
            maxHp = 18,
            armor = 0
        )
        val floor = DungeonGenerator.generateFloor(1, seed = 12345L)

        val result1 = CombatEngine.executePlayerAttack(player, enemy, SkillCatalog.strikeLight, floor.tiles)
        assertEquals(ComboStage.LIGHT_1, result1.comboAdvancedTo)
        assertTrue(result1.damageDealt > 0)
        assertTrue(enemy.currentHp < 18)

        val result2 = CombatEngine.executePlayerAttack(player, enemy, SkillCatalog.strikeLight, floor.tiles)
        assertEquals(ComboStage.LIGHT_2, result2.comboAdvancedTo)
    }
}
