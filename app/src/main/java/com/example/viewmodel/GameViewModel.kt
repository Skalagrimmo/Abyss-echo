package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScreenState {
    MAIN_MENU,
    DUNGEON,
    EVOLUTION_SCREEN,
    DILEMMA_POPUP,
    INVENTORY_SCREEN,
    CODEX_SCREEN,
    GAME_OVER
}

data class StoryRecord(
    val floor: Int,
    val title: String,
    val choiceMade: String,
    val consequence: String
)

data class GameUiState(
    val currentScreen: ScreenState = ScreenState.MAIN_MENU,
    val currentFloorNumber: Int = 1,
    val sectorTitle: String = "Затоплені Катакомби",
    val turnCount: Int = 0,
    val timePhase: TimePhase = TimePhase.TWILIGHT,
    val player: Player = Player(
        pos = GridPos(2, 2),
        currentHp = 90,
        maxHp = 90,
        armor = 2,
        sanity = 70
    ),
    val enemies: List<Enemy> = emptyList(),
    val targetEnemyId: String? = null,
    val selectedSkill: CombatSkill = SkillCatalog.strikeLight,
    val floatingFxList: List<FloatingFx> = emptyList(),
    val activeBanner: MilestoneBanner? = null,
    val availableInventory: List<Item> = listOf(
        ItemCatalog.salve.copy(quantity = 2),
        ItemCatalog.torchOil.copy(quantity = 2),
        ItemCatalog.smellingSalts.copy(quantity = 1)
    ),
    val activeDilemma: MoralDilemma? = null,
    val pendingHarvest: DefeatedHarvest? = null,
    val storyChronicle: List<StoryRecord> = emptyList(),
    val factionReputations: Map<Faction, Int> = mapOf(
        Faction.IRON_FOUNDRY to 0,
        Faction.CHTHONIC_ASCETICS to 0,
        Faction.FORGOTTEN_REMNANTS to 0
    ),
    val isVictory: Boolean = false,
    val deathReason: String = ""
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var currentDungeon: DungeonFloor? = null
        private set

    fun startNewGame(archetype: Archetype) {
        val player = Player(
            pos = GridPos(0, 0),
            currentHp = archetype.hp,
            maxHp = archetype.hp,
            armor = when (archetype) {
                Archetype.IRON_DESERTER -> 3
                Archetype.CHTHONIC_PENITENT -> 1
                Archetype.CHRONO_OUTCAST -> 0
            },
            sanity = archetype.sanity,
            scrap = archetype.scrap,
            biomass = archetype.biomass,
            archetype = archetype
        )

        player.knownSkills.add(SkillCatalog.strikeLight)
        player.knownSkills.add(SkillCatalog.strikeHeavy)
        player.knownSkills.add(SkillCatalog.throwTorch)

        when (archetype) {
            Archetype.IRON_DESERTER -> player.knownSkills.add(SkillCatalog.pistonSlam)
            Archetype.CHTHONIC_PENITENT -> player.knownSkills.add(SkillCatalog.siphonLash)
            Archetype.CHRONO_OUTCAST -> player.knownSkills.add(SkillCatalog.voidGaze)
        }

        loadFloor(1, player)
    }

    private fun loadFloor(floorNum: Int, playerOverride: Player? = null) {
        val floor = DungeonGenerator.generateFloor(floorNum)
        currentDungeon = floor

        val player = (playerOverride ?: _uiState.value.player).copy(
            pos = floor.playerStart,
            currentAp = 3
        )

        DungeonGenerator.calculateFov(floor.tiles, player.pos, player.fovRadius)

        _uiState.update { current ->
            current.copy(
                currentScreen = ScreenState.DUNGEON,
                currentFloorNumber = floorNum,
                sectorTitle = floor.sectorName,
                player = player,
                enemies = floor.enemies,
                targetEnemyId = floor.enemies.firstOrNull()?.id,
                activeDilemma = null,
                floatingFxList = emptyList(),
                activeBanner = MilestoneBanner(
                    text = floor.sectorName,
                    icon = "🏰",
                    colorHex = 0xFFF59E0B
                )
            )
        }

        SoundSynthesizer.playEldritchDrone()
    }

    fun movePlayer(deltaX: Int, deltaY: Int) {
        val dungeon = currentDungeon ?: return
        val currentP = _uiState.value.player
        if (currentP.isDead) return

        if (currentP.currentAp <= 0) {
            triggerBanner("Немає ОД! Завершіть хід", "⏳", 0xFFEF4444)
            return
        }

        val targetPos = GridPos(currentP.pos.x + deltaX, currentP.pos.y + deltaY)
        val width = dungeon.tiles.size
        val height = dungeon.tiles[0].size

        if (targetPos.x !in 0 until width || targetPos.y !in 0 until height) return

        // If enemy at target cell -> attack
        val enemyAtTarget = dungeon.enemies.find { it.pos == targetPos && !it.isDead }
        if (enemyAtTarget != null) {
            attackEnemy(enemyAtTarget, _uiState.value.selectedSkill)
            return
        }

        val tile = dungeon.tiles[targetPos.x][targetPos.y]
        if (!tile.isWalkable) return

        currentP.pos = targetPos
        currentP.currentAp--
        currentP.torchTurns = (currentP.torchTurns - 1).coerceAtLeast(0)

        SoundSynthesizer.playStep()

        // Loot handling
        if (tile.scrapLoot > 0 || tile.biomassLoot > 0) {
            currentP.scrap += tile.scrapLoot
            currentP.biomass += tile.biomassLoot
            addFloatingFx(
                gridX = targetPos.x,
                gridY = targetPos.y,
                text = "+${tile.scrapLoot} ⚙️  +${tile.biomassLoot} 🧬",
                colorHex = 0xFFF59E0B
            )
            tile.scrapLoot = 0
            tile.biomassLoot = 0
        }

        // Elevator exit
        if (tile.type == TileType.ELEVATOR_EXIT) {
            val nextFloor = _uiState.value.currentFloorNumber + 1
            if (nextFloor > 3) {
                _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER, isVictory = true) }
                return
            } else {
                loadFloor(nextFloor)
                return
            }
        }

        // Moral dilemma chance
        if (dungeon.pendingDilemma != null && Math.random() < 0.09) {
            val dilemma = dungeon.pendingDilemma
            dungeon.pendingDilemma = null
            _uiState.update { it.copy(activeDilemma = dilemma, currentScreen = ScreenState.DILEMMA_POPUP) }
            SoundSynthesizer.playEldritchDrone()
        }

        DungeonGenerator.calculateFov(dungeon.tiles, currentP.pos, currentP.fovRadius)
        tickTimeAndEntropy()

        _uiState.update { it.copy(player = currentP.copy()) }
    }

    fun selectSkill(skill: CombatSkill) {
        _uiState.update { it.copy(selectedSkill = skill) }
    }

    fun selectEnemyTarget(enemyId: String) {
        _uiState.update { it.copy(targetEnemyId = enemyId) }
    }

    fun attackEnemy(enemy: Enemy, skill: CombatSkill) {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead || enemy.isDead) return

        if (player.currentAp < skill.apCost) {
            triggerBanner("Потрібно ${skill.apCost} ОД!", "⚠️", 0xFFEF4444)
            return
        }

        val dist = player.pos.chebyshevDistance(enemy.pos)
        if (dist > skill.range) {
            triggerBanner("Поза досяжністю (${skill.range})", "🎯", 0xFF94A3B8)
            return
        }

        val result = CombatEngine.executePlayerAttack(player, enemy, skill, dungeon.tiles)

        // Visual damage popup over enemy
        addFloatingFx(
            gridX = enemy.pos.x,
            gridY = enemy.pos.y,
            text = if (result.isCritical) "💥 -${result.damageDealt}" else "-${result.damageDealt}",
            colorHex = if (result.isCritical) 0xFFF59E0B else 0xFFDC2626,
            isCrit = result.isCritical
        )

        // Visual heal popup over player if siphoned
        if (result.playerHealed > 0) {
            addFloatingFx(
                gridX = player.pos.x,
                gridY = player.pos.y,
                text = "+${result.playerHealed} ❤️",
                colorHex = 0xFF22C55E
            )
        }

        if (skill.id == SkillCatalog.strikeHeavy.id || skill.id == SkillCatalog.pistonSlam.id) {
            SoundSynthesizer.playHeavySlam()
        } else {
            SoundSynthesizer.playStrike()
        }

        if (result.enemyDied) {
            addFloatingFx(
                gridX = enemy.pos.x,
                gridY = enemy.pos.y,
                text = "💀 ЗНИЩЕНО",
                colorHex = 0xFFA855F7
            )
            val harvest = HarvestCatalog.generateHarvestFor(enemy.archetype)
            _uiState.update { it.copy(pendingHarvest = harvest) }
            SoundSynthesizer.playMutationChime()
        }

        if (player.isDead) {
            _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER, deathReason = "Загибель у бою") }
            return
        }

        _uiState.update { current ->
            current.copy(
                player = player.copy(),
                enemies = dungeon.enemies.filter { !it.isDead }
            )
        }
    }

    fun applyHarvestMutation(option: HarvestMutationOption) {
        val player = _uiState.value.player

        player.maxHp += option.bonusMaxHp
        if (option.bonusMaxHp > 0) {
            player.currentHp += option.bonusMaxHp
        }
        player.armor += option.bonusArmor
        player.maxAp += option.bonusAp
        player.currentAp += option.bonusAp
        player.fovRadius += option.bonusFov
        player.corruption = (player.corruption + option.corruptionDelta).coerceIn(0, player.maxCorruption)
        player.sanity = (player.sanity + option.sanityDelta).coerceIn(0, player.maxSanity)
        player.currentHp = (player.currentHp + option.healHp).coerceAtMost(player.maxHp)
        player.scrap += option.bonusScrap
        player.biomass += option.bonusBiomass

        if (option.grantedSkill != null && player.knownSkills.none { it.id == option.grantedSkill.id }) {
            player.knownSkills.add(option.grantedSkill)
        }

        val customMut = Mutation(
            id = "harvest_${System.currentTimeMillis()}",
            name = option.title,
            description = option.subtitle,
            category = option.category,
            slot = option.slot,
            tier = 1,
            biomassCost = 0,
            scrapCost = 0,
            sanityCost = 0,
            corruptionIncrease = option.corruptionDelta.coerceAtLeast(0),
            bonusMaxHp = option.bonusMaxHp,
            bonusArmor = option.bonusArmor,
            bonusAttackPower = option.bonusAttackPower,
            bonusAp = option.bonusAp,
            bonusFov = option.bonusFov,
            grantedSkillId = option.grantedSkill?.id,
            passiveDescription = option.passiveTrait
        )
        player.activeMutations.add(customMut)

        SoundSynthesizer.playMutationChime()
        triggerBanner("Прищеплено: ${option.title}", "🧬", 0xFFA855F7)

        _uiState.update { current ->
            current.copy(
                player = player.copy(),
                pendingHarvest = null
            )
        }
    }

    fun dismissHarvest() {
        _uiState.update { it.copy(pendingHarvest = null) }
    }

    fun endTurn() {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead) return

        // Process enemy turns cleanly
        for (enemy in dungeon.enemies.filter { !it.isDead }) {
            val result = CombatEngine.processEnemyTurn(enemy, player, dungeon.tiles)
            if (result.damageDealtToPlayer > 0) {
                addFloatingFx(
                    gridX = player.pos.x,
                    gridY = player.pos.y,
                    text = "-${result.damageDealtToPlayer} ❤️",
                    colorHex = 0xFFDC2626
                )
            }
            if (result.enemyDiedFromThorns) {
                val harvest = HarvestCatalog.generateHarvestFor(enemy.archetype)
                _uiState.update { it.copy(pendingHarvest = harvest) }
                SoundSynthesizer.playMutationChime()
            }
        }

        CombatEngine.tickStatusesAndEnvironment(player, dungeon.enemies, dungeon.tiles)

        var apRestored = player.maxAp
        if (player.activeMutations.any { it.id == "mut_clockwork_springs" } && _uiState.value.turnCount % 2 == 0) {
            apRestored++
        }
        player.currentAp = apRestored

        tickTimeAndEntropy()

        if (player.isDead) {
            _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER, deathReason = "Знекровлення у надрах") }
            return
        }

        _uiState.update { current ->
            current.copy(
                turnCount = current.turnCount + 1,
                player = player.copy(),
                enemies = dungeon.enemies.filter { !it.isDead }
            )
        }
    }

    private fun tickTimeAndEntropy() {
        val turns = _uiState.value.turnCount + 1
        val newPhase = when {
            turns > 90 -> TimePhase.AWAKENING_OF_GOD
            turns > 60 -> TimePhase.HOUR_OF_WHISPERS
            turns > 30 -> TimePhase.DEEPENING_GLOOM
            else -> TimePhase.TWILIGHT
        }

        val player = _uiState.value.player
        if (newPhase != _uiState.value.timePhase) {
            triggerBanner("Фаза: ${newPhase.title}", "⏱️", 0xFFA855F7)
        }

        if (newPhase == TimePhase.HOUR_OF_WHISPERS && turns % 15 == 0) {
            player.sanity = (player.sanity - 3).coerceAtLeast(0)
            addFloatingFx(player.pos.x, player.pos.y, "-3 🧠", 0xFF38BDF8)
        }
        if (player.torchTurns <= 0 && turns % 5 == 0) {
            player.sanity = (player.sanity - 2).coerceAtLeast(0)
            addFloatingFx(player.pos.x, player.pos.y, "Темрява! -2 🧠", 0xFF38BDF8)
        }

        _uiState.update { it.copy(turnCount = turns, timePhase = newPhase) }
    }

    fun chooseDilemmaOption(choice: DilemmaChoice) {
        val player = _uiState.value.player
        val activeD = _uiState.value.activeDilemma ?: return

        player.currentHp = (player.currentHp + choice.hpChange).coerceIn(1, player.maxHp)
        player.sanity = (player.sanity + choice.sanityChange).coerceIn(0, player.maxSanity)
        player.corruption = (player.corruption + choice.corruptionChange).coerceIn(0, player.maxCorruption)
        player.scrap = (player.scrap + choice.scrapChange).coerceAtLeast(0)
        player.biomass = (player.biomass + choice.biomassChange).coerceAtLeast(0)
        player.essence = (player.essence + choice.essenceChange).coerceAtLeast(0)

        if (choice.rewardMutationId != null) {
            val mut = MutationCatalog.allMutations.find { it.id == choice.rewardMutationId }
            if (mut != null && player.activeMutations.none { it.id == mut.id }) {
                acquireMutation(mut)
            }
        }

        val currentFactions = _uiState.value.factionReputations.toMutableMap()
        if (choice.factionAffected != null) {
            val curr = currentFactions[choice.factionAffected] ?: 0
            currentFactions[choice.factionAffected] = curr + choice.factionReputationChange
        }

        val record = StoryRecord(
            floor = _uiState.value.currentFloorNumber,
            title = activeD.title,
            choiceMade = choice.title,
            consequence = choice.consequenceStory
        )

        triggerBanner("Рішення: ${choice.title}", "⚖️", 0xFFF59E0B)

        _uiState.update { current ->
            current.copy(
                currentScreen = ScreenState.DUNGEON,
                activeDilemma = null,
                player = player.copy(),
                factionReputations = currentFactions,
                storyChronicle = current.storyChronicle + record
            )
        }
    }

    fun acquireMutation(mutation: Mutation) {
        val player = _uiState.value.player
        if (player.activeMutations.any { it.id == mutation.id }) return

        player.activeMutations.add(mutation)
        player.maxHp += mutation.bonusMaxHp
        player.currentHp = (player.currentHp + mutation.bonusMaxHp).coerceAtMost(player.maxHp)
        player.armor += mutation.bonusArmor
        player.fovRadius += mutation.bonusFov
        player.corruption = (player.corruption + mutation.corruptionIncrease).coerceAtMost(player.maxCorruption)

        if (mutation.grantedSkillId != null) {
            val skill = when (mutation.grantedSkillId) {
                "skill_siphon_lash" -> SkillCatalog.siphonLash
                "skill_piston_slam" -> SkillCatalog.pistonSlam
                "skill_steam_vent" -> SkillCatalog.steamVent
                "skill_blood_lance" -> SkillCatalog.bloodLance
                "skill_void_gaze" -> SkillCatalog.voidGaze
                else -> null
            }
            if (skill != null && player.knownSkills.none { it.id == skill.id }) {
                player.knownSkills.add(skill)
            }
        }

        SoundSynthesizer.playMutationChime()
        triggerBanner("Прищеплено: ${mutation.name}", "🧬", 0xFFA855F7)
        _uiState.update { it.copy(player = player.copy()) }
    }

    fun useItem(item: Item) {
        val player = _uiState.value.player
        val items = _uiState.value.availableInventory.toMutableList()
        val found = items.find { it.id == item.id } ?: return

        player.currentHp = (player.currentHp + item.hpRestore).coerceAtMost(player.maxHp)
        player.sanity = (player.sanity + item.sanityRestore).coerceAtMost(player.maxSanity)
        player.torchTurns += item.torchAdd
        player.corruption = (player.corruption - item.corruptionCleanse).coerceIn(0, player.maxCorruption)

        if (found.quantity > 1) {
            val idx = items.indexOf(found)
            items[idx] = found.copy(quantity = found.quantity - 1)
        } else {
            items.remove(found)
        }

        if (item.hpRestore > 0) addFloatingFx(player.pos.x, player.pos.y, "+${item.hpRestore} ❤️", 0xFF22C55E)
        if (item.sanityRestore > 0) addFloatingFx(player.pos.x, player.pos.y, "+${item.sanityRestore} 🧠", 0xFF38BDF8)
        if (item.torchAdd > 0) addFloatingFx(player.pos.x, player.pos.y, "+${item.torchAdd} 🔥", 0xFFF59E0B)

        _uiState.update { it.copy(player = player.copy(), availableInventory = items) }
    }

    fun craftItem(recipe: Recipe) {
        val player = _uiState.value.player
        if (player.scrap < recipe.scrapCost || player.biomass < recipe.biomassCost) {
            triggerBanner("Недостатньо ресурсів для крафту!", "⚠️", 0xFFEF4444)
            return
        }

        player.scrap -= recipe.scrapCost
        player.biomass -= recipe.biomassCost

        val items = _uiState.value.availableInventory.toMutableList()
        val existing = items.find { it.id == recipe.producedItem.id }
        if (existing != null) {
            val idx = items.indexOf(existing)
            items[idx] = existing.copy(quantity = existing.quantity + 1)
        } else {
            items.add(recipe.producedItem.copy(quantity = 1))
        }

        SoundSynthesizer.playHeavySlam()
        triggerBanner("Сковано: ${recipe.resultItemName}", "🛠️", 0xFFF59E0B)
        _uiState.update { it.copy(player = player.copy(), availableInventory = items) }
    }

    fun navigateTo(screen: ScreenState) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    private fun addFloatingFx(gridX: Int, gridY: Int, text: String, colorHex: Long, isCrit: Boolean = false) {
        val fx = FloatingFx(
            gridX = gridX,
            gridY = gridY,
            text = text,
            colorHex = colorHex,
            isCrit = isCrit
        )
        _uiState.update { current ->
            // Keep recent FX (pruning older than 1.8 seconds)
            val now = System.currentTimeMillis()
            val fresh = current.floatingFxList.filter { now - it.startTime < 1800 } + fx
            current.copy(floatingFxList = fresh)
        }
    }

    private fun triggerBanner(text: String, icon: String, colorHex: Long) {
        val banner = MilestoneBanner(text, icon, colorHex)
        _uiState.update { it.copy(activeBanner = banner) }
        viewModelScope.launch {
            delay(2800)
            _uiState.update { if (it.activeBanner == banner) it.copy(activeBanner = null) else it }
        }
    }
}
