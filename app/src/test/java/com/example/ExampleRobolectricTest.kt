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
    fun `dungeon generator calculates 16-bit autotiling front faces and wall edges`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 42L)
        var foundFrontFace = false
        var foundFloorVariation = false

        for (x in 0 until floor.width) {
            for (y in 0 until floor.height) {
                val tile = floor.tiles[x][y]
                if (tile.wallFrontFace) {
                    foundFrontFace = true
                    assertTrue(y + 1 < floor.height)
                    assertTrue(floor.tiles[x][y + 1].isWalkable)
                }
                if (tile.isWalkable && tile.floorVariation > 0) {
                    foundFloorVariation = true
                }
            }
        }

        assertTrue("Should generate front wall facades for 16-bit 3/4 perspective", foundFrontFace)
        assertTrue("Should generate decorative 16-bit floor variations", foundFloorVariation)
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

    @Test
    fun `harvest catalog generates valid biomechanical mutation choices on victory`() {
        val spiderHarvest = HarvestCatalog.generateHarvestFor(EnemyArchetype.CRAWLING_SPIDER)
        assertEquals("Повзуча Спороноска", spiderHarvest.enemyTitle)
        assertTrue(spiderHarvest.options.isNotEmpty())

        val titanHarvest = HarvestCatalog.generateHarvestFor(EnemyArchetype.CRUCIBLE_TITAN)
        assertEquals("Титан Горнила", titanHarvest.enemyTitle)
        assertTrue(titanHarvest.options.any { it.bonusMaxHp >= 40 })

        // Test mutating character attributes with harvested option
        val player = Player(
            pos = GridPos(0, 0),
            currentHp = 50,
            maxHp = 50,
            armor = 1,
            sanity = 50
        )
        val initialAtk = player.totalAttackPower()

        val automatonHarvest = HarvestCatalog.generateHarvestFor(EnemyArchetype.IRON_AUTOMATON)
        val armOption = automatonHarvest.options.first { it.bonusAttackPower > 0 }

        // Simulate grafting the mutation
        player.armor += armOption.bonusArmor
        val mut = Mutation(
            id = "test_graft",
            name = armOption.title,
            description = armOption.subtitle,
            category = armOption.category,
            slot = armOption.slot,
            tier = 1,
            biomassCost = 0,
            scrapCost = 0,
            sanityCost = 0,
            corruptionIncrease = armOption.corruptionDelta,
            bonusAttackPower = armOption.bonusAttackPower
        )
        player.activeMutations.add(mut)

        assertEquals(initialAtk + armOption.bonusAttackPower, player.totalAttackPower())
        if (armOption.grantedSkill != null) {
            player.knownSkills.add(armOption.grantedSkill)
            assertTrue(player.knownSkills.contains(armOption.grantedSkill))
        }
    }
}
