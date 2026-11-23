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
    val activeProps: List<TacticalProp> = emptyList(),
    val validTargetCells: Set<GridPos> = emptySet(),
    val blastFxPositions: List<GridPos> = emptyList(),
    val targetEnemyId: String? = null,
    val selectedSkill: CombatSkill = SkillCatalog.strikeLight,
    val floatingFxList: List<FloatingFx> = emptyList(),
    val activeAttackFx: List<AttackVisualFx> = emptyList(),
    val activeExplosions: List<ExplosionVisualFx> = emptyList(),
    val activeMutationSurges: List<MutationSurgeFx> = emptyList(),
    val activeSlotPulse: SlotIntegrationPulseFx? = null,
    val activeEnemyAnims: List<EnemyAnimFx> = emptyList(),
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
    val deathReason: String = "",
    val audioState: AudioUiState = AudioUiState()
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var currentDungeon: DungeonFloor? = null
        private set

    private var pendingReturnMutationSurge: Mutation? = null

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
        val initialTargets = CombatEngine.getValidTargetCells(
            player.pos,
            _uiState.value.selectedSkill,
            floor.tiles,
            floor.props
        )

        _uiState.update { current ->
            current.copy(
                currentScreen = ScreenState.DUNGEON,
                currentFloorNumber = floorNum,
                sectorTitle = floor.sectorName,
                player = player,
                enemies = floor.enemies,
                activeProps = floor.props.filter { !it.isDead },
                validTargetCells = initialTargets,
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
        syncDynamicAudio()
    }

    private fun refreshTacticalGrid(playerPos: GridPos? = null) {
        val dungeon = currentDungeon ?: return
        val pos = playerPos ?: _uiState.value.player.pos
        val skill = _uiState.value.selectedSkill
        val validTargets = CombatEngine.getValidTargetCells(pos, skill, dungeon.tiles, dungeon.props)
        _uiState.update { current ->
            current.copy(
                activeProps = dungeon.props.filter { !it.isDead },
                validTargetCells = validTargets,
                enemies = dungeon.enemies.filter { !it.isDead }
            )
        }
        syncDynamicAudio()
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
            executeAttack(enemyAtTarget, targetPos, _uiState.value.selectedSkill)
            return
        }

        // If blocking prop at target cell (barrel/crate) -> attack or alert
        val propAtTarget = dungeon.props.find { !it.isDead && it.pos == targetPos }
        if (propAtTarget != null && propAtTarget.type.blocksMovement) {
            // Attack the obstacle/barrel directly
            executeAttack(null, targetPos, _uiState.value.selectedSkill)
            return
        }

        val tile = dungeon.tiles[targetPos.x][targetPos.y]
        if (!tile.isWalkable) return

        currentP.pos = targetPos
        currentP.currentAp--
        currentP.torchTurns = (currentP.torchTurns - 1).coerceAtLeast(0)

        SoundSynthesizer.playStep()

        // Trap interaction (Spikes or Steam Vent)
        if (propAtTarget != null && !propAtTarget.type.blocksMovement) {
            when (propAtTarget.type) {
                TacticalPropType.SPIKE_PLATE -> {
                    currentP.currentHp = (currentP.currentHp - 6).coerceAtLeast(1)
                    currentP.statuses.add(StatusEffect(StatusEffectType.BLEEDING, 2))
                    addFloatingFx(targetPos.x, targetPos.y, "-6 ⚙️ ШИПИ!", 0xFFEF4444)
                    SoundSynthesizer.playStrike()
                }
                TacticalPropType.STEAM_PRESSURE_VENT -> {
                    currentP.currentHp = (currentP.currentHp - 4).coerceAtLeast(1)
                    currentP.statuses.add(StatusEffect(StatusEffectType.BURNING, 2))
                    addFloatingFx(targetPos.x, targetPos.y, "-4 ♨️ ПАРА!", 0xFFF59E0B)
                    SoundSynthesizer.playHeavySlam()
                }
                else -> {}
            }
        }

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
                syncDynamicAudio()
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
        refreshTacticalGrid(currentP.pos)
    }

    fun selectSkill(skill: CombatSkill) {
        _uiState.update { it.copy(selectedSkill = skill) }
        refreshTacticalGrid()
    }

    fun selectEnemyTarget(enemyId: String) {
        _uiState.update { it.copy(targetEnemyId = enemyId) }
    }

    fun handleTileClick(pos: GridPos) {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead) return

        val enemyAtPos = dungeon.enemies.find { !it.isDead && it.pos == pos }
        val propAtPos = dungeon.props.find { !it.isDead && it.pos == pos }
        val currentSkill = _uiState.value.selectedSkill
        val validTargets = _uiState.value.validTargetCells

        // If tapped cell is in tactical attack range
        if (validTargets.contains(pos)) {
            if (enemyAtPos != null) {
                selectEnemyTarget(enemyAtPos.id)
                executeAttack(enemyAtPos, pos, currentSkill)
                return
            } else if (propAtPos != null) {
                executeAttack(null, pos, currentSkill)
                return
            } else if (currentSkill.isAreaEffect) {
                executeAttack(null, pos, currentSkill)
                return
            }
        }

        // If clicked on an enemy outside current skill range, select target
        if (enemyAtPos != null) {
            selectEnemyTarget(enemyAtPos.id)
            return
        }

        // If adjacent cell and walkable, move player
        val dist = player.pos.chebyshevDistance(pos)
        if (dist == 1) {
            movePlayer(pos.x - player.pos.x, pos.y - player.pos.y)
        }
    }

    fun attackEnemy(enemy: Enemy, skill: CombatSkill) {
        executeAttack(enemy, enemy.pos, skill)
    }

    fun attackCell(targetPos: GridPos, skill: CombatSkill) {
        val dungeon = currentDungeon ?: return
        val enemy = dungeon.enemies.find { !it.isDead && it.pos == targetPos }
        executeAttack(enemy, targetPos, skill)
    }

    fun executeAttack(enemy: Enemy?, targetPos: GridPos, skill: CombatSkill) {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead) return

        if (player.currentAp < skill.apCost) {
            triggerBanner("Потрібно ${skill.apCost} ОД!", "⚠️", 0xFFEF4444)
            return
        }

        val validTargets = CombatEngine.getValidTargetCells(player.pos, skill, dungeon.tiles, dungeon.props)
        if (!validTargets.contains(targetPos)) {
            val dist = player.pos.chebyshevDistance(targetPos)
            if (dist > skill.range) {
                triggerBanner("Поза досяжністю (${skill.range})", "🎯", 0xFF94A3B8)
            } else {
                triggerBanner("Немає прямої видимості!", "🚫", 0xFFEF4444)
            }
            return
        }

        val result = CombatEngine.executePlayerAttack(
            player = player,
            enemy = enemy,
            targetPos = targetPos,
            skill = skill,
            tiles = dungeon.tiles,
            props = dungeon.props,
            enemies = dungeon.enemies
        )

        // Visual damage popup
        if (enemy != null && result.damageDealt > 0) {
            addFloatingFx(
                gridX = enemy.pos.x,
                gridY = enemy.pos.y,
                text = if (result.isCritical) "💥 -${result.damageDealt}" else "-${result.damageDealt}",
                colorHex = if (result.isCritical) 0xFFF59E0B else 0xFFDC2626,
                isCrit = result.isCritical
            )
        }

        // Trigger attack animation
        if (result.attackFx != null) {
            addAttackFx(result.attackFx)
        }
        for (enemyFx in result.enemyAnimFxList) {
            addEnemyAnimFx(enemyFx)
        }

        // Explosions FX and sounds
        if (result.explosionHits.isNotEmpty()) {
            SoundSynthesizer.playHeavySlam()
            for (exp in result.explosionFxList) {
                addExplosionFx(exp)
            }
            for (hitPos in result.explosionHits) {
                addFloatingFx(
                    gridX = hitPos.x,
                    gridY = hitPos.y,
                    text = "💥 БАБАХ!",
                    colorHex = 0xFFF59E0B,
                    isCrit = true
                )
            }
        }

        // Heal popup
        if (result.playerHealed > 0) {
            addFloatingFx(
                gridX = player.pos.x,
                gridY = player.pos.y,
                text = "+${result.playerHealed} ❤️",
                colorHex = 0xFF22C55E
            )
        }

        if (skill.id == SkillCatalog.strikeHeavy.id || skill.id == SkillCatalog.pistonSlam.id || result.explosionHits.isNotEmpty()) {
            SoundSynthesizer.playHeavySlam()
        } else {
            SoundSynthesizer.playStrike()
        }

        if (result.enemyDied && enemy != null) {
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
            syncDynamicAudio()
            return
        }

        _uiState.update { current ->
            current.copy(
                player = player.copy(),
                enemies = dungeon.enemies.filter { !it.isDead },
                activeProps = dungeon.props.filter { !it.isDead },
                blastFxPositions = result.explosionHits
            )
        }
        refreshTacticalGrid(player.pos)
    }

    fun applyHarvestMutation(option: HarvestMutationOption) {
        val harvest = _uiState.value.pendingHarvest
        val player = _uiState.value.player

        if (option.isResourceSalvage) {
            player.corruption = (player.corruption + option.corruptionDelta).coerceIn(0, player.maxCorruption)
            player.sanity = (player.sanity + option.sanityDelta).coerceIn(0, player.maxSanity)
            player.currentHp = (player.currentHp + option.healHp).coerceAtMost(player.maxHp)
            player.scrap += option.bonusScrap
            player.biomass += option.bonusBiomass

            SoundSynthesizer.playMutationChime()
            triggerBanner("Перероблено: ${option.title}", "⚗️", 0xFF22C55E)
            _uiState.update { current ->
                current.copy(
                    player = player.copy(),
                    pendingHarvest = null
                )
            }
            return
        }

        val mut = if (harvest != null) {
            EvolutionEngine.convertHarvestOptionToMutation(harvest, option)
        } else {
            Mutation(
                id = "organ_${option.trait.name.lowercase()}_${option.slot.name.lowercase()}",
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
                passiveDescription = option.passiveTrait ?: option.trait.passiveSummary,
                trait = option.trait
            )
        }

        val outcome = EvolutionEngine.integrateMutation(player, mut, deductCosts = false)
        if (!outcome.success) {
            // Slot already at Rank III: automatically salvage resources instead of losing trophy
            player.biomass += 10
            player.scrap += 6
            triggerBanner("Слот III рангу — утилізовано (+10 біо / +6 брухту)", "♻️", 0xFFF59E0B)
        } else {
            player.sanity = (player.sanity + option.sanityDelta).coerceIn(0, player.maxSanity)
            player.currentHp = (player.currentHp + option.healHp).coerceAtMost(player.maxHp)
            player.scrap += option.bonusScrap
            player.biomass += option.bonusBiomass

            val bannerText = if (outcome.unlockedSynergyTitle != null) {
                "${outcome.message} • Синергія: ${outcome.unlockedSynergyTitle}"
            } else {
                outcome.message
            }
            triggerBanner(bannerText, "🧬", branchColorHex(mut.category))
            emitOrganIntegrationFx(
                mutation = outcome.resultingMutation ?: mut,
                isUpgrade = outcome.actionType == EvolutionActionType.UPGRADED_RANK
            )
        }

        SoundSynthesizer.playMutationChime()
        _uiState.update { current ->
            current.copy(
                player = player.copy(),
                pendingHarvest = null
            )
        }
    }

    fun storeHarvestOrganInBioBank(option: HarvestMutationOption) {
        val harvest = _uiState.value.pendingHarvest ?: return
        if (option.isResourceSalvage) {
            applyHarvestMutation(option)
            return
        }
        val player = _uiState.value.player
        val stored = EvolutionEngine.storeHarvestedOrganInBioBank(player, harvest, option)
        SoundSynthesizer.playMutationChime()
        triggerBanner("У Біо-банк: ${stored.name}", "🧫", 0xFF06B6D4)

        _uiState.update { current ->
            current.copy(
                player = player.copy(),
                pendingHarvest = null
            )
        }
    }

    fun integrateBioBankOrgan(organId: String) {
        val player = _uiState.value.player
        val outcome = EvolutionEngine.integrateBioBankOrgan(player, organId)
        if (outcome.success) {
            SoundSynthesizer.playMutationChime()
            val resulting = outcome.resultingMutation
            val text = if (outcome.unlockedSynergyTitle != null) {
                "${outcome.message} • ${outcome.unlockedSynergyTitle}"
            } else {
                outcome.message
            }
            triggerBanner(text, "🧬", branchColorHex(resulting?.category ?: MutationCategory.CHTHONIC_FLESH))
            if (resulting != null) {
                emitOrganIntegrationFx(
                    mutation = resulting,
                    isUpgrade = outcome.actionType == EvolutionActionType.UPGRADED_RANK
                )
            }
        } else {
            triggerBanner(outcome.message, "⚠️", 0xFFF59E0B)
        }
        _uiState.update { it.copy(player = player.copy()) }
    }

    fun salvageBioBankOrgan(organId: String) {
        val player = _uiState.value.player
        val (bio, scrap) = EvolutionEngine.salvageBioBankOrgan(player, organId)
        if (bio > 0 || scrap > 0) {
            SoundSynthesizer.playStrike()
            triggerBanner("Утилізація: +$bio біомаси, +$scrap брухту", "♻️", 0xFF22C55E)
        }
        _uiState.update { it.copy(player = player.copy()) }
    }

    fun removeSlotMutation(slot: MutationSlot) {
        val player = _uiState.value.player
        val (bio, scrap) = EvolutionEngine.removeEquippedMutation(player, slot)
        if (bio > 0 || scrap > 0) {
            SoundSynthesizer.playStrike()
            triggerBanner("Вилучено слот [${slot.title}]: +$bio біо / +$scrap брухту", "🔬", 0xFF94A3B8)
        }
        _uiState.update { it.copy(player = player.copy()) }
    }

    fun dismissHarvest() {
        _uiState.update { it.copy(pendingHarvest = null) }
    }

    fun endTurn() {
        val dungeon = currentDungeon ?: return
        val player = _uiState.value.player
        if (player.isDead) return

        // Process enemy turns cleanly with props blocking
        for (enemy in dungeon.enemies.filter { !it.isDead }) {
            val result = CombatEngine.processEnemyTurn(enemy, player, dungeon.tiles, dungeon.props)
            if (result.attackFx != null) {
                addAttackFx(result.attackFx)
            }
            if (result.enemyAnimFx != null) {
                addEnemyAnimFx(result.enemyAnimFx)
            }
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

        CombatEngine.tickStatusesAndEnvironment(player, dungeon.enemies, dungeon.tiles, dungeon.props)

        var apRestored = player.maxAp
        val clockworkRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.CLOCKWORK_AP)
        val steelApex = EvolutionEngine.isApexSynergyActive(player, MutationCategory.IRON_CYBERNETICS)
        if (clockworkRank >= 3) {
            apRestored++
        } else if ((clockworkRank > 0 || steelApex) && _uiState.value.turnCount % 2 == 0) {
            apRestored++
        }
        player.currentAp = apRestored

        // Trigger biomechanical mutation surge effect for turn
        if (player.activeMutations.isNotEmpty()) {
            val turn = _uiState.value.turnCount + 1
            val activeMut = player.activeMutations[turn % player.activeMutations.size]
            val glyph = when (activeMut.slot) {
                MutationSlot.HEAD -> "👁️"
                MutationSlot.ARMS -> "⚔️"
                MutationSlot.TORSO -> "🛡️"
                MutationSlot.LEGS -> "⚡"
                MutationSlot.CORE -> "🧬"
            }
            val color = when (activeMut.slot) {
                MutationSlot.CORE -> 0xFF8B5CF6
                MutationSlot.ARMS -> 0xFFEF4444
                MutationSlot.TORSO -> 0xFFF59E0B
                MutationSlot.HEAD -> 0xFF06B6D4
                MutationSlot.LEGS -> 0xFF10B981
            }
            addMutationSurgeFx(
                MutationSurgeFx(
                    playerPos = player.pos,
                    mutationName = activeMut.name,
                    runeGlyph = glyph,
                    colorHex = color
                )
            )
        }

        tickTimeAndEntropy()

        if (player.isDead) {
            _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER, deathReason = "Знекровлення у надрах") }
            syncDynamicAudio()
            return
        }

        _uiState.update { current ->
            current.copy(
                turnCount = current.turnCount + 1,
                player = player.copy(),
                enemies = dungeon.enemies.filter { !it.isDead },
                activeProps = dungeon.props.filter { !it.isDead }
            )
        }
        refreshTacticalGrid(player.pos)
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
            if (mut != null) {
                acquireMutation(mut, deductCosts = false)
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

    fun acquireMutation(mutation: Mutation, deductCosts: Boolean = true) {
        val player = _uiState.value.player
        val outcome = EvolutionEngine.integrateMutation(player, mutation, deductCosts = deductCosts)
        if (outcome.success) {
            SoundSynthesizer.playMutationChime()
            val resulting = outcome.resultingMutation ?: mutation
            val text = if (outcome.unlockedSynergyTitle != null) {
                "${outcome.message} • ${outcome.unlockedSynergyTitle}"
            } else {
                outcome.message
            }
            triggerBanner(text, "🧬", branchColorHex(resulting.category))
            emitOrganIntegrationFx(
                mutation = resulting,
                isUpgrade = outcome.actionType == EvolutionActionType.UPGRADED_RANK
            )
            _uiState.update { it.copy(player = player.copy()) }
        } else {
            triggerBanner(outcome.message, "⚠️", 0xFFEF4444)
        }
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
        if (screen == ScreenState.DUNGEON) {
            val queued = pendingReturnMutationSurge
            if (queued != null) {
                pendingReturnMutationSurge = null
                triggerMapMutationSurge(queued)
            }
        }
        syncDynamicAudio()
    }

    fun setMusicVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        AmbientAudioEngine.setMusicVolume(clamped)
        _uiState.update { it.copy(audioState = it.audioState.copy(musicVolume = clamped)) }
    }

    fun setSfxVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        SoundSynthesizer.sfxVolume = clamped
        _uiState.update { it.copy(audioState = it.audioState.copy(sfxVolume = clamped)) }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.audioState.isMuted
        SoundSynthesizer.isMuted = nextMuted
        AmbientAudioEngine.setMuted(nextMuted)
        _uiState.update { it.copy(audioState = it.audioState.copy(isMuted = nextMuted)) }
    }

    fun applyPersistedAudioSettings(musicVolume: Float, sfxVolume: Float, isMuted: Boolean) {
        val clampedMusic = musicVolume.coerceIn(0f, 1f)
        val clampedSfx = sfxVolume.coerceIn(0f, 1f)
        SoundSynthesizer.sfxVolume = clampedSfx
        SoundSynthesizer.isMuted = isMuted
        AmbientAudioEngine.setMusicVolume(clampedMusic)
        AmbientAudioEngine.setMuted(isMuted)
        _uiState.update {
            it.copy(
                audioState = it.audioState.copy(
                    musicVolume = clampedMusic,
                    sfxVolume = clampedSfx,
                    isMuted = isMuted
                )
            )
        }
    }

    fun toggleAudioMixer() {
        _uiState.update {
            it.copy(audioState = it.audioState.copy(isMixerExpanded = !it.audioState.isMixerExpanded))
        }
    }

    fun dismissAudioMixer() {
        if (_uiState.value.audioState.isMixerExpanded) {
            _uiState.update {
                it.copy(audioState = it.audioState.copy(isMixerExpanded = false))
            }
        }
    }

    fun computeCombatIntensity(): Float {
        val state = _uiState.value
        val dungeon = currentDungeon ?: return 0f
        if (state.currentScreen == ScreenState.MAIN_MENU || state.currentScreen == ScreenState.GAME_OVER) {
            return 0f
        }

        val player = state.player
        val tiles = dungeon.tiles
        val aliveEnemies = dungeon.enemies.filter { !it.isDead }

        var intensity = 0f
        val visibleEnemies = aliveEnemies.filter { enemy ->
            val inBounds = enemy.pos.x in tiles.indices && enemy.pos.y in tiles[0].indices
            val isVisible = inBounds && tiles[enemy.pos.x][enemy.pos.y].isVisible
            val dist = player.pos.chebyshevDistance(enemy.pos)
            isVisible || dist <= 4
        }

        if (visibleEnemies.isNotEmpty()) {
            val minDist = visibleEnemies.minOf { player.pos.chebyshevDistance(it.pos) }
            val hasBoss = visibleEnemies.any {
                it.archetype == EnemyArchetype.CRUCIBLE_TITAN
            }
            intensity += when {
                hasBoss -> 0.85f
                minDist <= 2 -> 0.70f
                else -> 0.45f
            }
            intensity += ((visibleEnemies.size - 1) * 0.12f).coerceAtMost(0.30f)
        }

        val hpRatio = player.currentHp.toFloat() / player.maxHp.coerceAtLeast(1)
        if (hpRatio <= 0.35f) {
            intensity += 0.20f
        }

        intensity += when (state.timePhase) {
            TimePhase.HOUR_OF_WHISPERS -> 0.10f
            TimePhase.AWAKENING_OF_GOD -> 0.25f
            else -> 0f
        }

        return intensity.coerceIn(0f, 1f)
    }

    private fun resolveAudioProfile(): AmbientTrackProfile {
        val state = _uiState.value
        if (state.currentScreen == ScreenState.MAIN_MENU || state.currentScreen == ScreenState.GAME_OVER) {
            return AmbientTrackProfile.VOID_MENU
        }
        val biome = currentDungeon?.biome ?: DungeonGenerator.biomeForFloor(state.currentFloorNumber)
        return AmbientTrackProfile.fromBiome(biome)
    }

    private fun syncDynamicAudio() {
        val profile = resolveAudioProfile()
        val intensity = computeCombatIntensity()
        val audio = _uiState.value.audioState

        AmbientAudioEngine.setProfile(profile)
        AmbientAudioEngine.setCombatIntensity(intensity)
        AmbientAudioEngine.setMusicVolume(audio.musicVolume)
        AmbientAudioEngine.setMuted(audio.isMuted)
        SoundSynthesizer.sfxVolume = audio.sfxVolume
        SoundSynthesizer.isMuted = audio.isMuted

        _uiState.update { current ->
            current.copy(
                audioState = current.audioState.copy(
                    activeProfile = profile,
                    combatIntensity = intensity
                )
            )
        }
    }

    private fun branchColorHex(category: MutationCategory): Long = when (category) {
        MutationCategory.CHTHONIC_FLESH -> 0xFF22C55E
        MutationCategory.IRON_CYBERNETICS -> 0xFFF59E0B
        MutationCategory.OCCULT_SIGIL -> 0xFFA855F7
    }

    private fun branchGlyph(category: MutationCategory): String = when (category) {
        MutationCategory.CHTHONIC_FLESH -> "🧬"
        MutationCategory.IRON_CYBERNETICS -> "⚙️"
        MutationCategory.OCCULT_SIGIL -> "👁️"
    }

    private fun triggerMapMutationSurge(mutation: Mutation) {
        val player = _uiState.value.player
        addMutationSurgeFx(
            MutationSurgeFx(
                playerPos = player.pos,
                mutationName = mutation.name,
                runeGlyph = branchGlyph(mutation.category),
                colorHex = branchColorHex(mutation.category)
            )
        )
    }

    private fun emitOrganIntegrationFx(mutation: Mutation, isUpgrade: Boolean) {
        val pulse = SlotIntegrationPulseFx(
            slot = mutation.slot,
            category = mutation.category,
            isUpgrade = isUpgrade,
            tier = mutation.tier
        )
        _uiState.update { it.copy(activeSlotPulse = pulse) }
        viewModelScope.launch {
            delay(pulse.durationMs + 80)
            _uiState.update { current ->
                if (current.activeSlotPulse?.id == pulse.id) {
                    current.copy(activeSlotPulse = null)
                } else {
                    current
                }
            }
        }

        if (_uiState.value.currentScreen == ScreenState.EVOLUTION_SCREEN) {
            pendingReturnMutationSurge = mutation
        } else {
            triggerMapMutationSurge(mutation)
        }
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

    fun addAttackFx(fx: AttackVisualFx) {
        _uiState.update { current ->
            val now = System.currentTimeMillis()
            val fresh = current.activeAttackFx.filter { now - it.startTime < it.durationMs + 100 } + fx
            current.copy(activeAttackFx = fresh)
        }
        viewModelScope.launch {
            delay(fx.durationMs + 50)
            _uiState.update { current ->
                current.copy(activeAttackFx = current.activeAttackFx.filter { it.id != fx.id })
            }
        }
    }

    fun addExplosionFx(fx: ExplosionVisualFx) {
        _uiState.update { current ->
            val now = System.currentTimeMillis()
            val fresh = current.activeExplosions.filter { now - it.startTime < it.durationMs + 100 } + fx
            current.copy(activeExplosions = fresh)
        }
        viewModelScope.launch {
            delay(fx.durationMs + 50)
            _uiState.update { current ->
                current.copy(activeExplosions = current.activeExplosions.filter { it.id != fx.id })
            }
        }
    }

    fun addMutationSurgeFx(fx: MutationSurgeFx) {
        _uiState.update { current ->
            val now = System.currentTimeMillis()
            val fresh = current.activeMutationSurges.filter { now - it.startTime < it.durationMs + 100 } + fx
            current.copy(activeMutationSurges = fresh)
        }
        viewModelScope.launch {
            delay(fx.durationMs + 50)
            _uiState.update { current ->
                current.copy(activeMutationSurges = current.activeMutationSurges.filter { it.id != fx.id })
            }
        }
    }

    fun addEnemyAnimFx(fx: EnemyAnimFx) {
        _uiState.update { current ->
            val now = System.currentTimeMillis()
            val fresh = current.activeEnemyAnims.filter {
                now - it.startTime < it.durationMs + 100 && it.enemyId != fx.enemyId
            } + fx
            current.copy(activeEnemyAnims = fresh)
        }
        viewModelScope.launch {
            delay(fx.durationMs + 50)
            _uiState.update { current ->
                current.copy(activeEnemyAnims = current.activeEnemyAnims.filter { it.id != fx.id })
            }
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
