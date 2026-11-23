package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CombatEngine
import com.example.engine.DungeonGenerator
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class ExampleRobolectricTest {

    @Test
    fun `read string from resources matches app name`() {
        val stringsXml = File("src/main/res/values/strings.xml")
        val content = if (stringsXml.exists()) stringsXml.readText() else File("app/src/main/res/values/strings.xml").readText()
        assertTrue(content.contains("Ехо Безодні"))
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

        val result1 = CombatEngine.executePlayerAttack(
            player = player,
            enemy = enemy,
            targetPos = enemy.pos,
            skill = SkillCatalog.strikeLight,
            tiles = floor.tiles,
            props = floor.props,
            enemies = listOf(enemy)
        )
        assertEquals(ComboStage.LIGHT_1, result1.comboAdvancedTo)
        assertTrue(result1.damageDealt > 0)
        assertTrue(enemy.currentHp < 18)

        val result2 = CombatEngine.executePlayerAttack(
            player = player,
            enemy = enemy,
            targetPos = enemy.pos,
            skill = SkillCatalog.strikeLight,
            tiles = floor.tiles,
            props = floor.props,
            enemies = listOf(enemy)
        )
        assertEquals(ComboStage.LIGHT_2, result2.comboAdvancedTo)
    }

    @Test
    fun `line of sight correctly detects obstacles and blocking props`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 999L)
        val p1 = GridPos(5, 5)
        val p2 = GridPos(5, 8)

        // Clear line between (5,5) and (5,8) if floor tiles are walkable
        floor.tiles[5][5].type = TileType.FLOOR
        floor.tiles[5][6].type = TileType.FLOOR
        floor.tiles[5][7].type = TileType.FLOOR
        floor.tiles[5][8].type = TileType.FLOOR

        val props = mutableListOf<TacticalProp>()
        assertTrue(CombatEngine.hasLineOfSight(p1, p2, floor.tiles, props))

        // Add a blocking crate between them
        val crate = TacticalProp(
            id = "blocking_crate",
            pos = GridPos(5, 6),
            type = TacticalPropType.BARRICADE_CRATE
        )
        props.add(crate)

        assertFalse("Barricade crate should block line of sight", CombatEngine.hasLineOfSight(p1, p2, floor.tiles, props))
    }

    @Test
    fun `explosive barrel detonates and deals damage to surrounding entities`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 555L)
        val barrelPos = GridPos(10, 10)
        val enemyPos = GridPos(10, 11)
        val player = Player(
            pos = GridPos(10, 8), // Distance 2 away from barrel, safe from blast
            currentHp = 50,
            maxHp = 50,
            armor = 1,
            sanity = 50,
            currentAp = 3
        )
        val enemy = Enemy(
            id = "nearby_spider",
            archetype = EnemyArchetype.CRAWLING_SPIDER,
            pos = enemyPos,
            currentHp = 30,
            maxHp = 30
        )
        val barrel = TacticalProp(
            id = "barrel_1",
            pos = barrelPos,
            type = TacticalPropType.EXPLOSIVE_BARREL,
            currentHp = 5 // low HP, 1 strike will detonate
        )
        val props = mutableListOf(barrel)
        val enemies = listOf(enemy)

        // Direct attack on the barrel with strikeLight
        val result = CombatEngine.executePlayerAttack(
            player = player,
            enemy = null,
            targetPos = barrelPos,
            skill = SkillCatalog.strikeLight,
            tiles = floor.tiles,
            props = props,
            enemies = enemies
        )

        assertTrue("Barrel should be destroyed", barrel.isDead)
        assertTrue("Explosion hits should be recorded", result.explosionHits.isNotEmpty())
        assertTrue("Enemy should take splash damage from barrel explosion", enemy.currentHp < 30)
        assertEquals(TileType.EMBER_HEARTH, floor.tiles[barrelPos.x][barrelPos.y].type)
    }

    @Test
    fun `tactical grid computes correct valid target cells based on distance type`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 777L)
        val playerPos = GridPos(10, 10)
        val props = mutableListOf<TacticalProp>()

        // Melee skill (range 1)
        val meleeTargets = CombatEngine.getValidTargetCells(playerPos, SkillCatalog.strikeLight, floor.tiles, props)
        for (cell in meleeTargets) {
            assertEquals(1, playerPos.chebyshevDistance(cell))
        }

        // Reach skill (range 2)
        val reachTargets = CombatEngine.getValidTargetCells(playerPos, SkillCatalog.siphonLash, floor.tiles, props)
        for (cell in reachTargets) {
            assertTrue(playerPos.chebyshevDistance(cell) in 1..2)
        }

        // Ranged skill (range 4)
        val rangedTargets = CombatEngine.getValidTargetCells(playerPos, SkillCatalog.throwTorch, floor.tiles, props)
        for (cell in rangedTargets) {
            assertTrue(playerPos.chebyshevDistance(cell) in 1..4)
        }
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

    @Test
    fun `combat attack produces 16-bit transition AttackVisualFx and barrel explosion FX`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 101L)
        val player = Player(pos = GridPos(5, 5), currentHp = 60, maxHp = 60, armor = 2, sanity = 50)
        val enemy = Enemy(
            id = "test_target",
            archetype = EnemyArchetype.CRAWLING_SPIDER,
            pos = GridPos(5, 6),
            currentHp = 20,
            maxHp = 20
        )
        val barrel = TacticalProp(
            id = "test_barrel",
            pos = GridPos(5, 7),
            type = TacticalPropType.EXPLOSIVE_BARREL,
            currentHp = 1
        )
        val props = mutableListOf(barrel)

        // 1. Melee attack produces MELEE AttackVisualFx
        val meleeResult = CombatEngine.executePlayerAttack(
            player = player,
            enemy = enemy,
            targetPos = enemy.pos,
            skill = SkillCatalog.strikeLight,
            tiles = floor.tiles,
            props = props,
            enemies = listOf(enemy)
        )
        assertNotNull(meleeResult.attackFx)
        assertEquals(AttackDistanceType.MELEE, meleeResult.attackFx?.distanceType)
        assertEquals(player.pos, meleeResult.attackFx?.from)
        assertEquals(enemy.pos, meleeResult.attackFx?.to)

        // 2. Direct attack on barrel produces both RANGED AttackVisualFx and ExplosionVisualFx
        val rangedResult = CombatEngine.executePlayerAttack(
            player = player,
            enemy = null,
            targetPos = barrel.pos,
            skill = SkillCatalog.throwTorch,
            tiles = floor.tiles,
            props = props,
            enemies = listOf(enemy)
        )
        assertNotNull(rangedResult.attackFx)
        assertEquals(AttackDistanceType.RANGED, rangedResult.attackFx?.distanceType)
        assertTrue("Explosion visual FX should be generated for detonated barrel", rangedResult.explosionFxList.isNotEmpty())
        assertEquals(barrel.pos, rangedResult.explosionFxList[0].epicenter)
        assertTrue(rangedResult.explosionFxList[0].debris.isNotEmpty())
    }

    @Test
    fun `endTurn triggers mutation surge animation for equipped biomechanical mutations`() {
        val vm = com.example.viewmodel.GameViewModel()
        vm.startNewGame(Archetype.IRON_DESERTER)

        val player = vm.uiState.value.player
        val testMutation = Mutation(
            id = "test_clockwork",
            name = "Годинникові пружини",
            description = "Збільшує відновлення ОД",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.CORE,
            tier = 1,
            biomassCost = 0,
            scrapCost = 0,
            sanityCost = 0,
            corruptionIncrease = 5
        )
        player.activeMutations.add(testMutation)

        // Ending turn with active mutations should trigger a MutationSurgeFx
        vm.endTurn()

        val activeSurges = vm.uiState.value.activeMutationSurges
        assertTrue("Active mutation surges should be triggered on turn end", activeSurges.isNotEmpty())
        assertEquals(player.pos, activeSurges.last().playerPos)
        assertEquals(testMutation.name, activeSurges.last().mutationName)
    }

    @Test
    fun `enemy animation phases emit HURT on damage and ATTACK on enemy strike`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 202L)
        val player = Player(pos = GridPos(5, 5), currentHp = 80, maxHp = 80, armor = 2, sanity = 60)
        val enemy = Enemy(
            id = "anim_spider",
            archetype = EnemyArchetype.CRAWLING_SPIDER,
            pos = GridPos(5, 6),
            currentHp = 40,
            maxHp = 40
        )

        // 1. Player attack triggers HURT animation on target enemy
        val attackRes = CombatEngine.executePlayerAttack(
            player = player,
            enemy = enemy,
            targetPos = enemy.pos,
            skill = SkillCatalog.strikeLight,
            tiles = floor.tiles,
            props = mutableListOf(),
            enemies = listOf(enemy)
        )
        assertEquals(1, attackRes.enemyAnimFxList.size)
        assertEquals("anim_spider", attackRes.enemyAnimFxList[0].enemyId)
        assertEquals(EnemyAnimPhase.HURT, attackRes.enemyAnimFxList[0].phase)

        // 2. Enemy turn attack triggers ATTACK lunge animation toward player
        val enemyTurnRes = CombatEngine.processEnemyTurn(
            enemy = enemy,
            player = player,
            tiles = floor.tiles,
            props = emptyList()
        )
        assertNotNull(enemyTurnRes.enemyAnimFx)
        assertEquals("anim_spider", enemyTurnRes.enemyAnimFx?.enemyId)
        assertEquals(EnemyAnimPhase.ATTACK, enemyTurnRes.enemyAnimFx?.phase)
        assertEquals(0, enemyTurnRes.enemyAnimFx?.dirX)
        assertEquals(-1, enemyTurnRes.enemyAnimFx?.dirY)
    }

    @Test
    fun `sprite sheet processor removes white background and slices 3 distinct poses`() {
        val w = 90
        val h = 20
        val white = 0xFFFFFFFF.toInt()
        val dark = 0xFF1A1A1A.toInt()
        val pixels = IntArray(w * h) { white }

        // Draw 3 separated sprite blocks (Idle, Attack, Hurt) with an interior white eye pixel
        fun fillBlock(xRange: IntRange, yRange: IntRange) {
            for (y in yRange) {
                for (x in xRange) {
                    pixels[y * w + x] = dark
                }
            }
        }
        fillBlock(6..20, 4..16)
        pixels[8 * w + 12] = white // Interior white eye must be preserved!
        fillBlock(36..52, 3..16)
        fillBlock(66..82, 5..16)

        val bounds = com.example.ui.screen.EnemySpriteCache.processSheetPixelsAndFindBounds(pixels, w, h)
        assertNotNull("Should slice 3 distinct frames from sprite sheet", bounds)
        val (idle, attack, hurt) = bounds!!
        assertEquals(6, idle.minX)
        assertEquals(20, idle.maxX)
        assertEquals(36, attack.minX)
        assertEquals(52, attack.maxX)
        assertEquals(66, hurt.minX)
        assertEquals(82, hurt.maxX)

        // Exterior corner should be transparent, interior white eye should stay opaque
        assertEquals(0, pixels[0])
        assertEquals(white, pixels[8 * w + 12])
    }

    @Test
    fun `dungeon generator assigns distinct biomechanical tileset per depth tier`() {
        val f1 = DungeonGenerator.generateFloor(1, seed = 313L)
        val f2 = DungeonGenerator.generateFloor(2, seed = 313L)
        val f3 = DungeonGenerator.generateFloor(3, seed = 313L)
        val f4 = DungeonGenerator.generateFloor(4, seed = 313L)

        assertEquals(TilesetBiome.FLESH, f1.biome)
        assertEquals(TilesetBiome.STEEL, f2.biome)
        assertEquals(TilesetBiome.CRUCIBLE, f3.biome)
        assertEquals(TilesetBiome.CRUCIBLE, f4.biome)

        assertEquals(TilesetBiome.FLESH, f1.tiles[f1.playerStart.x][f1.playerStart.y].biome)
        assertEquals(TilesetBiome.STEEL, f2.tiles[f2.playerStart.x][f2.playerStart.y].biome)
        assertEquals(TilesetBiome.CRUCIBLE, f3.tiles[f3.playerStart.x][f3.playerStart.y].biome)
    }

    @Test
    fun `4-bit wallMask and conduitMask match neighboring tile state and guarantee player to exit BFS path`() {
        for (floorNum in 1..3) {
            val floor = DungeonGenerator.generateFloor(floorNum, seed = 9000L + floorNum)
            val w = floor.width
            val h = floor.height

            fun isWall(type: TileType): Boolean =
                type == TileType.VOID || type == TileType.WALL || type == TileType.IRON_WALL

            var hasConduits = false
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val t = floor.tiles[x][y]
                    if (isWall(t.type)) {
                        var expectedMask = 0
                        if (y - 1 < 0 || isWall(floor.tiles[x][y - 1].type)) expectedMask = expectedMask or 1
                        if (x + 1 >= w || isWall(floor.tiles[x + 1][y].type)) expectedMask = expectedMask or 2
                        if (y + 1 >= h || isWall(floor.tiles[x][y + 1].type)) expectedMask = expectedMask or 4
                        if (x - 1 < 0 || isWall(floor.tiles[x - 1][y].type)) expectedMask = expectedMask or 8

                        assertEquals("wallMask mismatch at ($x,$y)", expectedMask, t.wallMask)
                        assertEquals((expectedMask and 4) == 0 && y + 1 < h, t.wallFrontFace)
                        assertEquals(expectedMask == 0, t.isPillar)
                    } else {
                        assertEquals(0, t.wallMask)
                        assertFalse(t.wallFrontFace)
                        if (t.decal == BiomechDecal.FLESH_VEINS ||
                            t.decal == BiomechDecal.COPPER_PIPE ||
                            t.decal == BiomechDecal.RUNIC_SEAM
                        ) {
                            hasConduits = true
                            var expectedConduit = 0
                            if (y - 1 >= 0 && floor.tiles[x][y - 1].decal == t.decal) expectedConduit = expectedConduit or 1
                            if (x + 1 < w && floor.tiles[x + 1][y].decal == t.decal) expectedConduit = expectedConduit or 2
                            if (y + 1 < h && floor.tiles[x][y + 1].decal == t.decal) expectedConduit = expectedConduit or 4
                            if (x - 1 >= 0 && floor.tiles[x - 1][y].decal == t.decal) expectedConduit = expectedConduit or 8
                            assertEquals("conduitMask mismatch at ($x,$y)", expectedConduit, t.conduitMask)
                        }
                    }
                }
            }
            assertTrue("Expected biomechanical floor conduits on floor $floorNum", hasConduits)

            // Verify BFS walkable reachability from playerStart to exitPos
            val visited = Array(w) { BooleanArray(h) }
            val queue = java.util.ArrayDeque<GridPos>()
            queue.add(floor.playerStart)
            visited[floor.playerStart.x][floor.playerStart.y] = true
            var reachedExit = false
            while (queue.isNotEmpty()) {
                val cur = queue.poll() ?: continue
                if (cur == floor.exitPos) {
                    reachedExit = true
                    break
                }
                for (n in cur.adjacentNeighbors(includeDiagonals = false)) {
                    if (n.x in 0 until w && n.y in 0 until h && !visited[n.x][n.y] && floor.tiles[n.x][n.y].isWalkable) {
                        visited[n.x][n.y] = true
                        queue.add(n)
                    }
                }
            }
            assertTrue("Player start must reach elevator exit via walkable BFS path on floor $floorNum", reachedExit)
        }
    }

    @Test
    fun `biomechanical room stamps preserve walkable perimeter and room center`() {
        val w = 15
        val h = 15
        val tiles = Array(w) { x ->
            Array(h) { y ->
                Tile(pos = GridPos(x, y), type = TileType.FLOOR, biome = TilesetBiome.STEEL)
            }
        }
        val roomX = 3
        val roomY = 3
        val roomW = 9
        val roomH = 9
        val stamp = com.example.engine.BiomechRoomStamps.stamps.first { it.id == "steel_piston_bay" }
        com.example.engine.BiomechRoomStamps.applyStampToRoom(
            tiles = tiles,
            roomX = roomX,
            roomY = roomY,
            roomW = roomW,
            roomH = roomH,
            biome = TilesetBiome.STEEL,
            stamp = stamp,
            random = kotlin.random.Random(42L)
        )

        // Room center must remain walkable
        assertTrue(tiles[roomX + roomW / 2][roomY + roomH / 2].isWalkable)
        // Outer perimeter of room must remain walkable
        for (rx in roomX until roomX + roomW) {
            assertTrue(tiles[rx][roomY].isWalkable)
            assertTrue(tiles[rx][roomY + roomH - 1].isWalkable)
        }
        for (ry in roomY until roomY + roomH) {
            assertTrue(tiles[roomX][ry].isWalkable)
            assertTrue(tiles[roomX + roomW - 1][ry].isWalkable)
        }
    }

    @Test
    fun `evolution engine supports bio-bank storage, integration, and salvage`() {
        val player = Player(pos = GridPos(2, 2), currentHp = 80, maxHp = 80, armor = 2, sanity = 60, biomass = 10, scrap = 10)
        val harvest = HarvestCatalog.generateHarvestFor(EnemyArchetype.IRON_AUTOMATON)
        val armOption = harvest.options.first { !it.isResourceSalvage && it.slot == MutationSlot.ARMS }

        val stored = com.example.engine.EvolutionEngine.storeHarvestedOrganInBioBank(player, harvest, armOption)
        assertEquals(1, player.harvestedOrgans.size)
        assertEquals(armOption.title, stored.name)

        // Integrate from Bio-Bank -> equips in ARMS slot at Rank I and removes from Bio-Bank
        val outcome = com.example.engine.EvolutionEngine.integrateBioBankOrgan(player, stored.id)
        assertTrue(outcome.success)
        assertEquals(0, player.harvestedOrgans.size)
        assertEquals(1, player.activeMutations.size)
        assertEquals(1, player.activeMutations[0].tier)
        assertEquals(10 + armOption.bonusAttackPower, player.totalAttackPower())

        // Store second organ and salvage it instead
        val torsoOption = harvest.options.first { !it.isResourceSalvage && it.slot == MutationSlot.TORSO }
        val stored2 = com.example.engine.EvolutionEngine.storeHarvestedOrganInBioBank(player, harvest, torsoOption)
        val prevBio = player.biomass
        val prevScrap = player.scrap
        val (gainedBio, gainedScrap) = com.example.engine.EvolutionEngine.salvageBioBankOrgan(player, stored2.id)
        assertTrue(gainedBio > 0 && gainedScrap > 0)
        assertEquals(prevBio + gainedBio, player.biomass)
        assertEquals(prevScrap + gainedScrap, player.scrap)
        assertTrue(player.harvestedOrgans.isEmpty())
    }

    @Test
    fun `evolution engine upgrades matching slot up to rank 3 and cleanly replaces conflicting organs`() {
        val player = Player(pos = GridPos(2, 2), currentHp = 80, maxHp = 80, armor = 2, sanity = 70)
        val chitin = MutationCatalog.allMutations.first { it.id == "mut_chitin_carapace" } // TORSO +20 HP, +3 Armor

        // Rank I
        val res1 = com.example.engine.EvolutionEngine.integrateMutation(player, chitin, deductCosts = false)
        assertEquals(com.example.engine.EvolutionActionType.INSTALLED_NEW, res1.actionType)
        assertEquals(1, res1.resultingMutation?.tier)
        assertEquals(100, player.maxHp)
        assertEquals(5, player.totalArmor())

        // Rank II upgrade
        val res2 = com.example.engine.EvolutionEngine.integrateMutation(player, chitin, deductCosts = false)
        assertEquals(com.example.engine.EvolutionActionType.UPGRADED_RANK, res2.actionType)
        assertEquals(2, res2.resultingMutation?.tier)
        assertEquals(110, player.maxHp) // base 80 + (20 * 1.5 = 30)
        assertEquals(6, player.totalArmor()) // base 2 + (3 * 1.5 = 4)

        // Rank III upgrade
        val res3 = com.example.engine.EvolutionEngine.integrateMutation(player, chitin, deductCosts = false)
        assertEquals(com.example.engine.EvolutionActionType.UPGRADED_RANK, res3.actionType)
        assertEquals(3, res3.resultingMutation?.tier)
        assertEquals(120, player.maxHp) // base 80 + (20 * 2 = 40)
        assertEquals(8, player.totalArmor()) // base 2 + (3 * 2 = 6)

        // Fourth attempt should report MAX_RANK_REACHED
        val res4 = com.example.engine.EvolutionEngine.integrateMutation(player, chitin, deductCosts = false)
        assertFalse(res4.success)
        assertEquals(com.example.engine.EvolutionActionType.MAX_RANK_REACHED, res4.actionType)

        // Replace TORSO with Iron Exoskeleton (+5 Armor, +0 HP) -> cleanly removes Chitin HP without stat leak
        val exo = MutationCatalog.allMutations.first { it.id == "mut_plated_exoskeleton" }
        val replaceRes = com.example.engine.EvolutionEngine.integrateMutation(player, exo, deductCosts = false)
        assertEquals(com.example.engine.EvolutionActionType.REPLACED_EXISTING, replaceRes.actionType)
        assertEquals(1, player.activeMutations.count { it.slot == MutationSlot.TORSO })
        assertEquals(80, player.maxHp) // back to base 80!
        assertEquals(7, player.totalArmor()) // base 2 + 5 exoskeleton, no double counting!
    }

    @Test
    fun `branch synergies and passive combat traits activate properly in combat`() {
        val floor = DungeonGenerator.generateFloor(1, seed = 4242L)
        val player = Player(pos = GridPos(5, 5), currentHp = 80, maxHp = 80, armor = 2, sanity = 60, biomass = 200, scrap = 200)

        // Equip 3 Chthonic Flesh organs -> triggers both Minor (2+) and Apex (3+) Flesh Synergies
        val fleshArms = MutationCatalog.allMutations.first { it.id == "mut_siphon_tendrils" }
        val fleshTorso = MutationCatalog.allMutations.first { it.id == "mut_chitin_carapace" }
        val fleshLegs = MutationCatalog.allMutations.first { it.id == "mut_acid_glands" }
        com.example.engine.EvolutionEngine.integrateMutation(player, fleshArms, deductCosts = true)
        com.example.engine.EvolutionEngine.integrateMutation(player, fleshTorso, deductCosts = true)
        val apexOutcome = com.example.engine.EvolutionEngine.integrateMutation(player, fleshLegs, deductCosts = true)

        assertTrue(apexOutcome.success)
        assertNotNull(apexOutcome.unlockedSynergyTitle)
        assertTrue(com.example.engine.EvolutionEngine.isMinorSynergyActive(player, MutationCategory.CHTHONIC_FLESH))
        assertTrue(com.example.engine.EvolutionEngine.isApexSynergyActive(player, MutationCategory.CHTHONIC_FLESH))
        assertTrue(player.biomass < 200) // Lab costs were actually deducted!

        // Test Chitin Thorns passive in enemy turn
        val spider = Enemy(
            id = "thorns_target",
            archetype = EnemyArchetype.CRAWLING_SPIDER,
            pos = GridPos(5, 6),
            currentHp = 3,
            maxHp = 18,
            armor = 0
        )
        val turnRes = CombatEngine.processEnemyTurn(spider, player, floor.tiles, emptyList())
        assertTrue("Chitin carapace thorns should kill 3 HP attacker", turnRes.enemyDiedFromThorns)
        assertTrue(spider.isDead)
    }

    @Test
    fun `organ integration emits branch-colored slot pulse and synchronized map surge`() {
        val vm = com.example.viewmodel.GameViewModel()
        vm.startNewGame(Archetype.IRON_DESERTER)

        // Navigate into Evolution Laboratory and graft a Steel Cybernetic arm mutation
        vm.navigateTo(com.example.viewmodel.ScreenState.EVOLUTION_SCREEN)
        val piston = MutationCatalog.allMutations.first { it.id == "mut_hydraulic_piston" } // IRON_CYBERNETICS
        vm.acquireMutation(piston, deductCosts = false)

        val pulse = vm.uiState.value.activeSlotPulse
        assertNotNull("Slot integration should emit activeSlotPulse", pulse)
        assertEquals(MutationSlot.ARMS, pulse?.slot)
        assertEquals(MutationCategory.IRON_CYBERNETICS, pulse?.category)
        assertFalse(pulse?.isUpgrade ?: true)

        // Upgrade same slot to Rank II -> should mark pulse.isUpgrade = true
        vm.acquireMutation(piston, deductCosts = false)
        val upgradePulse = vm.uiState.value.activeSlotPulse
        assertNotNull(upgradePulse)
        assertTrue("Second integration of same lineage should emit upgrade pulse", upgradePulse?.isUpgrade == true)
        assertEquals(2, upgradePulse?.tier)

        // Returning from Evolution Lab to Dungeon should emit synchronized MutationSurgeFx in Steel amber color
        vm.navigateTo(com.example.viewmodel.ScreenState.DUNGEON)
        val surges = vm.uiState.value.activeMutationSurges
        assertTrue("Returning to dungeon after lab integration should trigger synchronized map surge", surges.isNotEmpty())
        assertEquals(0xFFF59E0B, surges.last().colorHex)
    }

    @Test
    fun `ambient drone synthesizer generates distinct tier drones with smooth crossfade and combat surge`() {
        val synth = com.example.engine.AmbientDroneSynthesizer(sampleRate = 22050)
        synth.setMusicVolume(0.8f)

        val fleshBuf = ShortArray(2048)
        synth.setProfile(AmbientTrackProfile.FLESH_CATACOMBS)
        // Render past crossfade (2s = 44100 samples)
        repeat(24) { synth.renderBuffer(fleshBuf) }
        assertEquals(1f, synth.currentCrossfadeProgress, 0.001f)
        assertTrue("Flesh drone must emit non-silent PCM waveform", fleshBuf.any { kotlin.math.abs(it.toInt()) > 200 })

        // Crossfade to Steel Foundry starts smoothly from 0f and finishes at 1f
        synth.setProfile(AmbientTrackProfile.STEEL_FOUNDRY)
        val transitionBuf = ShortArray(1024)
        synth.renderBuffer(transitionBuf)
        assertTrue("Crossfade should progress gradually over time", synth.currentCrossfadeProgress in 0.01f..0.99f)

        repeat(45) { synth.renderBuffer(transitionBuf) }
        assertEquals(1f, synth.currentCrossfadeProgress, 0.001f)

        // Switch to Crucible Abyss and increase combat intensity
        synth.setProfile(AmbientTrackProfile.CRUCIBLE_ABYSS)
        synth.setCombatIntensity(1.0f)
        repeat(45) { synth.renderBuffer(transitionBuf) }
        assertTrue("Smoothed combat intensity should rise near 1.0", synth.currentSmoothedIntensity > 0.85f)
        assertTrue("Crucible combat drone must emit strong signal", transitionBuf.any { kotlin.math.abs(it.toInt()) > 500 })

        // Muting smoothly drives output to zero
        synth.setMuted(true)
        val muteBuf = ShortArray(2048)
        repeat(8) { synth.renderBuffer(muteBuf) }
        val peakMuted = muteBuf.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Muted drone output should decay to silence (peak=$peakMuted)", peakMuted < 20)
    }

    @Test
    fun `game viewmodel updates ambient track profile per tier, combat surge, and independent volumes`() {
        val vm = com.example.viewmodel.GameViewModel()
        assertEquals(AmbientTrackProfile.VOID_MENU, vm.uiState.value.audioState.activeProfile)

        // Start game -> Floor 1 (FLESH)
        vm.startNewGame(Archetype.IRON_DESERTER)
        assertEquals(AmbientTrackProfile.FLESH_CATACOMBS, vm.uiState.value.audioState.activeProfile)
        assertEquals(AmbientTrackProfile.fromBiome(TilesetBiome.STEEL), AmbientTrackProfile.STEEL_FOUNDRY)
        assertEquals(AmbientTrackProfile.fromBiome(TilesetBiome.CRUCIBLE), AmbientTrackProfile.CRUCIBLE_ABYSS)

        // Independent Music & SFX volume controls
        vm.setMusicVolume(0.35f)
        vm.setSfxVolume(0.95f)
        assertEquals(0.35f, vm.uiState.value.audioState.musicVolume, 0.001f)
        assertEquals(0.95f, vm.uiState.value.audioState.sfxVolume, 0.001f)
        assertEquals(0.95f, com.example.engine.SoundSynthesizer.sfxVolume, 0.001f)

        // Toggle HUD mixer overlay & mute state
        vm.toggleAudioMixer()
        assertTrue(vm.uiState.value.audioState.isMixerExpanded)
        vm.toggleMute()
        assertTrue(vm.uiState.value.audioState.isMuted)
        vm.toggleMute()
        assertFalse(vm.uiState.value.audioState.isMuted)
        vm.dismissAudioMixer()
        assertFalse(vm.uiState.value.audioState.isMixerExpanded)

        // Nearby visible enemy elevates combat intensity above surge threshold
        val dungeon = vm.currentDungeon!!
        val player = vm.uiState.value.player
        dungeon.enemies.clear()
        dungeon.enemies.add(
            Enemy(
                id = "close_threat",
                archetype = EnemyArchetype.CRUCIBLE_TITAN,
                pos = GridPos(player.pos.x, player.pos.y + 1),
                currentHp = 50,
                maxHp = 50
            )
        )
        vm.endTurn()
        assertTrue(
            "Visible boss threat should activate combat surge intensity",
            vm.uiState.value.audioState.combatIntensity >= 0.7f
        )
        assertTrue(vm.uiState.value.audioState.isCombatSurgeActive)

        // Returning to Main Menu switches profile back to VOID_MENU with 0 combat intensity
        vm.navigateTo(com.example.viewmodel.ScreenState.MAIN_MENU)
        assertEquals(AmbientTrackProfile.VOID_MENU, vm.uiState.value.audioState.activeProfile)
        assertEquals(0f, vm.uiState.value.audioState.combatIntensity, 0.001f)
    }
}



