package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.model.*
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
    val logs: List<String> = listOf("Ласкаво просимо до Безодні. Кожен крок відраховує темний час."),
    val availableInventory: List<Item> = listOf(
        ItemCatalog.salve.copy(quantity = 2),
        ItemCatalog.torchOil.copy(quantity = 2),
        ItemCatalog.smellingSalts.copy(quantity = 1)
    ),
    val activeDilemma: MoralDilemma? = null,
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

        // Starting abilities
        player.knownSkills.add(SkillCatalog.strikeLight)
        player.knownSkills.add(SkillCatalog.strikeHeavy)
        player.knownSkills.add(SkillCatalog.throwTorch)

        when (archetype) {
            Archetype.IRON_DESERTER -> {
                player.knownSkills.add(SkillCatalog.pistonSlam)
            }
            Archetype.CHTHONIC_PENITENT -> {
                player.knownSkills.add(SkillCatalog.siphonLash)
            }
            Archetype.CHRONO_OUTCAST -> {
                player.knownSkills.add(SkillCatalog.voidGaze)
            }
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
                logs = listOf("Вхід у сектор: ${floor.sectorName}. Метал дрижить від тиску.") + current.logs.take(15)
            )
        }

        SoundSynthesizer.playEldritchDrone()
    }

    fun movePlayer(deltaX: Int, deltaY: Int) {
        val dungeon = currentDungeon ?: return
        val currentP = _uiState.value.player
        if (currentP.isDead) return

        if (currentP.currentAp <= 0) {
            addLog("⚠️ Бракує пунктів дії (ОД)! Завершіть хід.")
            return
        }

        val targetPos = GridPos(currentP.pos.x + deltaX, currentP.pos.y + deltaY)
        val width = dungeon.tiles.size
        val height = dungeon.tiles[0].size

        if (targetPos.x !in 0 until width || targetPos.y !in 0 until height) return

        // Check if an enemy is at the target cell -> attack instead!
        val enemyAtTarget = dungeon.enemies.find { it.pos == targetPos && !it.isDead }
        if (enemyAtTarget != null) {
            attackEnemy(enemyAtTarget, _uiState.value.selectedSkill)
            return
        }

        val tile = dungeon.tiles[targetPos.x][targetPos.y]
        if (!tile.isWalkable) {
            return
        }

        // Execute movement
        currentP.pos = targetPos
        currentP.currentAp--
        currentP.torchTurns = (currentP.torchTurns - 1).coerceAtLeast(0)

        SoundSynthesizer.playStep()

        // Check loot
        if (tile.scrapLoot > 0 || tile.biomassLoot > 0) {
            currentP.scrap += tile.scrapLoot
            currentP.biomass += tile.biomassLoot
            addLog("💰 Підібрано залишки: +${tile.scrapLoot} Брухту, +${tile.biomassLoot} Біомаси.")
            tile.scrapLoot = 0
            tile.biomassLoot = 0
        }

        // Elevator exit check
        if (tile.type == TileType.ELEVATOR_EXIT) {
            val nextFloor = _uiState.value.currentFloorNumber + 1
            if (nextFloor > 3) {
                // Victory over the Factory God!
                _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER, isVictory = true) }
                return
            } else {
                addLog("⏫ Шахта ліфта гуркоче, опускаючи вас глибше у безодню...")
                loadFloor(nextFloor)
                return
            }
        }

        // Random chance of triggering sector's moral dilemma on discovery
        if (dungeon.pendingDilemma != null && Math.random() < 0.08) {
            val dilemma = dungeon.pendingDilemma
            dungeon.pendingDilemma = null
            _uiState.update { it.copy(activeDilemma = dilemma, currentScreen = ScreenState.DILEMMA_POPUP) }
            SoundSynthesizer.playEldritchDrone()
        }

        // Recalculate FOV
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
            addLog("⚠️ Недостатньо ОД для [${skill.name}]! Потрібно ${skill.apCost} ОД.")
            return
        }

        val dist = player.pos.chebyshevDistance(enemy.pos)
        if (dist > skill.range) {
            addLog("⚠️ Ціль занадто далеко для [${skill.name}] (Дальність: ${skill.range}).")
            return
        }

        val result = CombatEngine.executePlayerAttack(player, enemy, skill, dungeon.tiles)
        result.logs.forEach { addLog(it) }

        if (skill.id == SkillCatalog.strikeHeavy.id || skill.id == SkillCatalog.pistonSlam.id) {
            SoundSynthesizer.playHeavySlam()
        } else {
            SoundSynthesizer.playStrike()
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

    fun endTurn() {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead) return

        addLog("⏳ Хід передано ворогам...")

        // Enemy turns
        val enemyLogs = mutableListOf<String>()
        for (enemy in dungeon.enemies.filter { !it.isDead }) {
            val logs = CombatEngine.processEnemyTurn(enemy, player, dungeon.tiles)
            enemyLogs.addAll(logs)
        }
        enemyLogs.forEach { addLog(it) }

        // Environmental status ticks
        val envLogs = CombatEngine.tickStatusesAndEnvironment(player, dungeon.enemies, dungeon.tiles)
        envLogs.forEach { addLog(it) }

        // Restore Player AP
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

        // Sanity drain during whispers
        val player = _uiState.value.player
        if (newPhase == TimePhase.HOUR_OF_WHISPERS && turns % 15 == 0) {
            player.sanity = (player.sanity - 3).coerceAtLeast(0)
            addLog("🌀 Прадавній шепіт розриває думки... -3 Глузду.")
        }
        if (player.torchTurns <= 0 && turns % 5 == 0) {
            player.sanity = (player.sanity - 2).coerceAtLeast(0)
            addLog("🌑 Непроглядна темрява сіє жах! -2 Глузду.")
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

        // Award mutation if given
        if (choice.rewardMutationId != null) {
            val mut = MutationCatalog.allMutations.find { it.id == choice.rewardMutationId }
            if (mut != null && player.activeMutations.none { it.id == mut.id }) {
                acquireMutation(mut)
            }
        }

        // Update faction
        val currentFactions = _uiState.value.factionReputations.toMutableMap()
        if (choice.factionAffected != null) {
            val curr = currentFactions[choice.factionAffected] ?: 0
            currentFactions[choice.factionAffected] = curr + choice.factionReputationChange
        }

        // Record chronicle
        val record = StoryRecord(
            floor = _uiState.value.currentFloorNumber,
            title = activeD.title,
            choiceMade = choice.title,
            consequence = choice.consequenceStory
        )

        addLog("📜 ВИБІР: ${choice.title}")
        addLog(choice.consequenceStory)

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
                addLog("✨ Розблоковано бойову здатність: [${skill.name}]!")
            }
        }

        SoundSynthesizer.playMutationChime()
        addLog("🧬 Тіло зазнало мутації: [${mutation.name}].")
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

        addLog("🧪 Використано: ${item.name}.")
        _uiState.update { it.copy(player = player.copy(), availableInventory = items) }
    }

    fun craftItem(recipe: Recipe) {
        val player = _uiState.value.player
        if (player.scrap < recipe.scrapCost || player.biomass < recipe.biomassCost) {
            addLog("⚠️ Недостатньо ресурсів для створення!")
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

        addLog("🛠️ Створено: [${recipe.resultItemName}].")
        SoundSynthesizer.playHeavySlam()
        _uiState.update { it.copy(player = player.copy(), availableInventory = items) }
    }

    fun navigateTo(screen: ScreenState) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    private fun addLog(text: String) {
        _uiState.update { current ->
            current.copy(logs = listOf(text) + current.logs.take(25))
        }
    }
}
