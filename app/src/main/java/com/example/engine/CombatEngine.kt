package com.example.engine

import com.example.model.*
import kotlin.math.abs
import kotlin.random.Random

data class CombatResult(
    val damageDealt: Int,
    val isCritical: Boolean,
    val enemyDied: Boolean,
    val comboAdvancedTo: ComboStage,
    val playerHealed: Int = 0,
    val statusInflicted: StatusEffectType? = null,
    val propsDestroyed: List<TacticalProp> = emptyList(),
    val explosionHits: List<GridPos> = emptyList(),
    val attackFx: AttackVisualFx? = null,
    val explosionFxList: List<ExplosionVisualFx> = emptyList(),
    val enemyAnimFxList: List<EnemyAnimFx> = emptyList(),
    val logs: List<String>
)

data class EnemyTurnResult(
    val damageDealtToPlayer: Int = 0,
    val enemyDiedFromThorns: Boolean = false,
    val attackFx: AttackVisualFx? = null,
    val enemyAnimFx: EnemyAnimFx? = null,
    val logs: List<String> = emptyList()
)

object CombatEngine {

    /**
     * Checks if line-of-sight between from and to is clear using Bresenham's line algorithm.
     * Blocks line of sight if any intermediate tile is not transparent, or contains a prop that blocks LoS.
     */
    fun hasLineOfSight(
        from: GridPos,
        to: GridPos,
        tiles: Array<Array<Tile>>,
        props: List<TacticalProp>
    ): Boolean {
        var x0 = from.x
        var y0 = from.y
        val x1 = to.x
        val y1 = to.y

        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        val w = tiles.size
        val h = tiles[0].size

        while (true) {
            if (x0 == x1 && y0 == y1) break

            // Skip origin cell
            if (x0 != from.x || y0 != from.y) {
                if (x0 !in 0 until w || y0 !in 0 until h) return false
                if (!tiles[x0][y0].isTransparent) return false

                // Check obstacle props that block LoS (like crates or barrels)
                val blockingProp = props.find { !it.isDead && it.pos.x == x0 && it.pos.y == y0 && it.type.blocksLineOfSight }
                if (blockingProp != null) return false
            }

            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x0 += sx
            }
            if (e2 < dx) {
                err += dx
                y0 += sy
            }
        }
        return true
    }

    /**
     * Computes all valid targeted cells for a given skill from player position.
     */
    fun getValidTargetCells(
        playerPos: GridPos,
        skill: CombatSkill,
        tiles: Array<Array<Tile>>,
        props: List<TacticalProp>
    ): Set<GridPos> {
        val validCells = mutableSetOf<GridPos>()
        val w = tiles.size
        val h = tiles[0].size
        val r = skill.range

        for (dx in -r..r) {
            for (dy in -r..r) {
                if (dx == 0 && dy == 0) continue
                val targetPos = GridPos(playerPos.x + dx, playerPos.y + dy)
                if (targetPos.x !in 0 until w || targetPos.y !in 0 until h) continue

                val dist = playerPos.chebyshevDistance(targetPos)
                if (dist > r) continue

                when (skill.distanceType) {
                    AttackDistanceType.MELEE -> {
                        // Exactly distance 1
                        if (dist == 1) {
                            validCells.add(targetPos)
                        }
                    }
                    AttackDistanceType.REACH -> {
                        // Range 1 to 2, can pierce through low obstacles
                        if (dist in 1..2) {
                            validCells.add(targetPos)
                        }
                    }
                    AttackDistanceType.RANGED -> {
                        // Requires line-of-sight
                        if (dist in 1..r) {
                            if (hasLineOfSight(playerPos, targetPos, tiles, props)) {
                                validCells.add(targetPos)
                            }
                        }
                    }
                    AttackDistanceType.AREA -> {
                        // Target any cell within range
                        if (dist in 1..r) {
                            validCells.add(targetPos)
                        }
                    }
                }
            }
        }
        return validCells
    }

    fun executePlayerAttack(
        player: Player,
        enemy: Enemy?,
        targetPos: GridPos,
        skill: CombatSkill,
        tiles: Array<Array<Tile>>,
        props: MutableList<TacticalProp>,
        enemies: List<Enemy>
    ): CombatResult {
        val logs = mutableListOf<String>()
        val random = Random.Default
        var healed = 0
        val destroyedProps = mutableListOf<TacticalProp>()
        val explosionPositions = mutableListOf<GridPos>()
        val enemyAnimFxList = mutableListOf<EnemyAnimFx>()

        // Deduct Action Points (AP)
        player.currentAp = (player.currentAp - skill.apCost).coerceAtLeast(0)

        // Self HP cost (e.g. blood lance)
        if (skill.selfHpCost > 0) {
            player.currentHp = (player.currentHp - skill.selfHpCost).coerceAtLeast(1)
        }

        // Combo & Evolution Crit calculation
        var comboMultiplier = 1.0f
        var critChance = 0.15f
        if (player.sanity < 30) critChance += 0.25f
        if (EvolutionEngine.isMinorSynergyActive(player, MutationCategory.CHTHONIC_FLESH)) {
            critChance += 0.12f
        }
        val voidCritRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.VOID_CRIT_DARKNESS)
        if (voidCritRank > 0 && (player.torchTurns <= 25 || player.sanity < 50)) {
            critChance += 0.10f + (0.10f * voidCritRank)
        }
        var isCrit = random.nextFloat() < critChance.coerceAtMost(0.85f)

        val nextStage = when (skill.id) {
            SkillCatalog.strikeLight.id -> {
                when (player.comboStage) {
                    ComboStage.READY -> {
                        player.comboHits = 1
                        ComboStage.LIGHT_1
                    }
                    ComboStage.LIGHT_1 -> {
                        player.comboHits = 2
                        comboMultiplier = 1.25f
                        ComboStage.LIGHT_2
                    }
                    ComboStage.LIGHT_2 -> {
                        player.comboHits = 3
                        comboMultiplier = 1.5f
                        ComboStage.HEAVY_PRIMED
                    }
                    ComboStage.HEAVY_PRIMED -> ComboStage.HEAVY_PRIMED
                }
            }
            SkillCatalog.strikeHeavy.id -> {
                if (player.comboStage == ComboStage.HEAVY_PRIMED || player.comboHits >= 2) {
                    comboMultiplier = 2.0f
                    isCrit = true
                }
                player.comboHits = 0
                ComboStage.READY
            }
            else -> {
                player.comboHits = 0
                ComboStage.READY
            }
        }
        player.comboStage = nextStage

        // Blood Frenzy & Occult Sigil synergy multipliers
        var evolutionDmgMult = 1.0f
        val frenzyRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.BLOOD_FRENZY)
        if (frenzyRank > 0 && player.maxHp > 0) {
            val missingRatio = (1f - (player.currentHp.toFloat() / player.maxHp.toFloat())).coerceIn(0f, 1f)
            val maxBonus = when (frenzyRank) {
                1 -> 0.50f
                2 -> 0.75f
                else -> 1.00f
            }
            evolutionDmgMult += missingRatio * maxBonus
        }
        if (skill.id != SkillCatalog.strikeLight.id &&
            EvolutionEngine.isMinorSynergyActive(player, MutationCategory.OCCULT_SIGIL)
        ) {
            evolutionDmgMult += 0.15f
        }
        if (EvolutionEngine.isApexSynergyActive(player, MutationCategory.OCCULT_SIGIL)) {
            evolutionDmgMult += 0.20f
        }

        // Calculate total damage
        val baseAtk = player.totalAttackPower()
        var rawDmg = ((skill.baseDamage + (baseAtk / 2)) * comboMultiplier * evolutionDmgMult).toInt()
        if (isCrit) rawDmg = (rawDmg * 1.5f).toInt()

        var netDmg = rawDmg
        var enemyDied = false

        // 1. Direct Enemy Attack
        if (enemy != null && !enemy.isDead) {
            netDmg = (rawDmg - enemy.armor).coerceAtLeast(2)
            enemy.currentHp -= netDmg
            enemyAnimFxList.add(
                EnemyAnimFx(
                    enemyId = enemy.id,
                    phase = EnemyAnimPhase.HURT,
                    dirX = enemy.pos.x - player.pos.x,
                    dirY = enemy.pos.y - player.pos.y
                )
            )

            // Knockback logic (from skill OR PISTON_KNOCKBACK trait on melee attacks)
            val pistonRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.PISTON_KNOCKBACK)
            val shouldPush = skill.pushesTarget ||
                (pistonRank > 0 && skill.distanceType == AttackDistanceType.MELEE)
            if (shouldPush) {
                val dir = player.pos.directionTo(enemy.pos)
                val pushTargetPos = GridPos(enemy.pos.x + dir.x, enemy.pos.y + dir.y)
                val width = tiles.size
                val height = tiles[0].size

                if (pushTargetPos.x in 0 until width && pushTargetPos.y in 0 until height) {
                    val targetTile = tiles[pushTargetPos.x][pushTargetPos.y]
                    val propAtTarget = props.find { !it.isDead && it.pos == pushTargetPos }

                    if (!targetTile.isWalkable) {
                        // Smashed into wall!
                        val wallDmg = 8 + (pistonRank * 2)
                        enemy.currentHp -= wallDmg
                        enemy.statuses.add(StatusEffect(StatusEffectType.STUNNED, 1))
                    } else if (propAtTarget != null) {
                        // Smashed into prop / barrel!
                        propAtTarget.currentHp -= (10 + pistonRank * 2)
                        if (propAtTarget.isDead) {
                            destroyedProps.add(propAtTarget)
                            if (propAtTarget.type.isExplosive) {
                                triggerPropExplosion(propAtTarget, tiles, props, enemies, player, explosionPositions, enemyAnimFxList)
                            }
                        }
                        enemy.statuses.add(StatusEffect(StatusEffectType.STUNNED, 1))
                    } else {
                        enemy.pos = pushTargetPos
                    }
                }
            }

            if (skill.appliesStatus != null) {
                enemy.statuses.add(StatusEffect(skill.appliesStatus, skill.statusTurns))
            }

            // Toxic Crit passive trait (also granted by Chthonic Flesh Apex 3+)
            val toxicCritRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.TOXIC_CRIT)
            if (toxicCritRank > 0 && (isCrit || (toxicCritRank >= 2 && random.nextFloat() < 0.45f))) {
                enemy.statuses.add(StatusEffect(StatusEffectType.POISONED, 2 + toxicCritRank))
                logs.add("Токсичні протоки отруїли ворога!")
            }

            val fleshMinorBonus = if (EvolutionEngine.isMinorSynergyActive(player, MutationCategory.CHTHONIC_FLESH)) 1.25f else 1.0f

            if (skill.healsSelf > 0) {
                val skillHeal = (skill.healsSelf * fleshMinorBonus).toInt()
                player.currentHp = (player.currentHp + skillHeal).coerceAtMost(player.maxHp)
                healed += skillHeal
            }

            // Siphon Crit passive trait (scaled by rank + Flesh synergy)
            val siphonRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.SIPHON_CRIT)
            if (siphonRank > 0 && isCrit) {
                val siphonHeal = ((2 + siphonRank * 2) * fleshMinorBonus).toInt()
                player.currentHp = (player.currentHp + siphonHeal).coerceAtMost(player.maxHp)
                healed += siphonHeal
            }

            // Occult Apex Synergy: +3 Sanity on Crit
            if (isCrit && EvolutionEngine.isApexSynergyActive(player, MutationCategory.OCCULT_SIGIL)) {
                player.sanity = (player.sanity + 3).coerceAtMost(player.maxSanity)
            }

            if (enemy.isDead) {
                enemyDied = true
                player.scrap += enemy.archetype.scrapReward
                player.biomass += enemy.archetype.biomassReward
                tiles[enemy.pos.x][enemy.pos.y].hasBloodSplatter = true

                val soulRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.SOUL_SIPHON)
                if (soulRank > 0) {
                    val sanityGain = 1 + (soulRank * 2)
                    player.sanity = (player.sanity + sanityGain).coerceAtMost(player.maxSanity)
                }

                if (EvolutionEngine.isApexSynergyActive(player, MutationCategory.CHTHONIC_FLESH)) {
                    player.currentHp = (player.currentHp + 4).coerceAtMost(player.maxHp)
                    healed += 4
                }
            }
        }

        // 2. Direct Prop / Barrel Attack
        val directProp = props.find { !it.isDead && it.pos == targetPos }
        if (directProp != null) {
            directProp.currentHp -= rawDmg
            if (directProp.isDead) {
                destroyedProps.add(directProp)
                if (directProp.type.isExplosive) {
                    triggerPropExplosion(directProp, tiles, props, enemies, player, explosionPositions, enemyAnimFxList)
                }
            }
        }

        // 3. Area of Effect (AoE) Splash Damage
        if (skill.isAreaEffect) {
            val radius = skill.areaRadius
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    val aoePos = GridPos(targetPos.x + dx, targetPos.y + dy)
                    if (aoePos == targetPos) continue

                    // Damage adjacent enemies in AoE
                    val aoeEnemy = enemies.find { !it.isDead && it.pos == aoePos }
                    if (aoeEnemy != null) {
                        val splashDmg = (rawDmg * 0.75f).toInt()
                        aoeEnemy.currentHp -= splashDmg
                        enemyAnimFxList.add(
                            EnemyAnimFx(
                                enemyId = aoeEnemy.id,
                                phase = EnemyAnimPhase.HURT,
                                dirX = aoeEnemy.pos.x - targetPos.x,
                                dirY = aoeEnemy.pos.y - targetPos.y
                            )
                        )
                        if (aoeEnemy.isDead) {
                            tiles[aoePos.x][aoePos.y].hasBloodSplatter = true
                        }
                    }

                    // Detonate / damage adjacent props
                    val aoeProp = props.find { !it.isDead && it.pos == aoePos }
                    if (aoeProp != null) {
                        aoeProp.currentHp -= rawDmg
                        if (aoeProp.isDead) {
                            destroyedProps.add(aoeProp)
                            if (aoeProp.type.isExplosive) {
                                triggerPropExplosion(aoeProp, tiles, props, enemies, player, explosionPositions, enemyAnimFxList)
                            }
                        }
                    }
                }
            }
        }

        val attackColor = when (skill.distanceType) {
            AttackDistanceType.MELEE -> 0xFFEF4444
            AttackDistanceType.REACH -> 0xFF06B6D4
            AttackDistanceType.RANGED -> 0xFFF59E0B
            AttackDistanceType.AREA -> 0xFF8B5CF6
        }
        val attackFx = AttackVisualFx(
            from = player.pos,
            to = targetPos,
            distanceType = skill.distanceType,
            colorHex = attackColor
        )

        val explosionFxList = explosionPositions.distinct().map { pos ->
            ExplosionVisualFx(epicenter = pos)
        }

        return CombatResult(
            damageDealt = netDmg,
            isCritical = isCrit,
            enemyDied = enemyDied,
            comboAdvancedTo = nextStage,
            playerHealed = healed,
            statusInflicted = skill.appliesStatus,
            propsDestroyed = destroyedProps,
            explosionHits = explosionPositions,
            attackFx = attackFx,
            explosionFxList = explosionFxList,
            enemyAnimFxList = enemyAnimFxList,
            logs = logs
        )
    }

    /**
     * Triggers explosive barrel detonation with chain reaction onto adjacent barrels and entities.
     */
    private fun triggerPropExplosion(
        barrel: TacticalProp,
        tiles: Array<Array<Tile>>,
        props: MutableList<TacticalProp>,
        enemies: List<Enemy>,
        player: Player,
        explosionHits: MutableList<GridPos>,
        enemyAnimFxList: MutableList<EnemyAnimFx> = mutableListOf()
    ) {
        explosionHits.add(barrel.pos)
        val w = tiles.size
        val h = tiles[0].size
        val r = barrel.type.explosionRadius
        val dmg = barrel.type.explosionDamage

        // Leave scorched slag fire on barrel tile
        tiles[barrel.pos.x][barrel.pos.y].type = TileType.EMBER_HEARTH

        for (dx in -r..r) {
            for (dy in -r..r) {
                val ex = barrel.pos.x + dx
                val ey = barrel.pos.y + dy
                if (ex !in 0 until w || ey !in 0 until h) continue
                val pos = GridPos(ex, ey)
                explosionHits.add(pos)

                // Damage enemies in explosion blast
                val enemyInBlast = enemies.find { !it.isDead && it.pos == pos }
                if (enemyInBlast != null) {
                    enemyInBlast.currentHp -= dmg
                    enemyInBlast.statuses.add(StatusEffect(StatusEffectType.BURNING, 3))
                    enemyAnimFxList.add(
                        EnemyAnimFx(
                            enemyId = enemyInBlast.id,
                            phase = EnemyAnimPhase.HURT,
                            dirX = enemyInBlast.pos.x - barrel.pos.x,
                            dirY = enemyInBlast.pos.y - barrel.pos.y
                        )
                    )
                    if (enemyInBlast.isDead) {
                        tiles[pos.x][pos.y].hasBloodSplatter = true
                    }
                }

                // Damage player if caught in blast
                if (player.pos == pos) {
                    player.currentHp = (player.currentHp - (dmg / 2)).coerceAtLeast(0)
                    player.statuses.add(StatusEffect(StatusEffectType.BURNING, 2))
                }

                // Chain reaction: detonate other explosive barrels
                val chainedBarrel = props.find { !it.isDead && it.pos == pos && it.type.isExplosive && it.id != barrel.id }
                if (chainedBarrel != null) {
                    chainedBarrel.currentHp = 0
                    chainedBarrel.isDestroyed = true
                    triggerPropExplosion(chainedBarrel, tiles, props, enemies, player, explosionHits, enemyAnimFxList)
                }
            }
        }
    }

    fun processEnemyTurn(
        enemy: Enemy,
        player: Player,
        tiles: Array<Array<Tile>>,
        props: List<TacticalProp>
    ): EnemyTurnResult {
        if (enemy.isDead) return EnemyTurnResult(0, false, null, null, emptyList())

        // Check stun
        val stun = enemy.statuses.find { it.type == StatusEffectType.STUNNED }
        if (stun != null) {
            return EnemyTurnResult(0, false, null, null, emptyList())
        }

        val dist = enemy.pos.chebyshevDistance(player.pos)
        var damageToPlayer = 0
        var enemyDied = false
        var attackFx: AttackVisualFx? = null
        var enemyAnimFx: EnemyAnimFx? = null

        if (dist <= enemy.archetype.range) {
            val rawDamage = enemy.archetype.baseDamage
            val ironRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.IRON_MITIGATION)
            val ironBonus = if (ironRank > 0) 1 + (ironRank * 2) else 0
            val steelMinor = if (EvolutionEngine.isMinorSynergyActive(player, MutationCategory.IRON_CYBERNETICS)) 2 else 0
            val defense = player.totalArmor() + ironBonus + steelMinor
            damageToPlayer = (rawDamage - defense).coerceAtLeast(1)

            player.currentHp = (player.currentHp - damageToPlayer).coerceAtLeast(0)

            attackFx = AttackVisualFx(
                from = enemy.pos,
                to = player.pos,
                distanceType = if (enemy.archetype.range > 1) AttackDistanceType.RANGED else AttackDistanceType.MELEE,
                colorHex = 0xFFDC2626
            )
            enemyAnimFx = EnemyAnimFx(
                enemyId = enemy.id,
                phase = EnemyAnimPhase.ATTACK,
                dirX = player.pos.x - enemy.pos.x,
                dirY = player.pos.y - enemy.pos.y,
                durationMs = 360L
            )

            // Thorns from CHITIN_THORNS trait + Iron Cybernetics Apex Synergy (3+)
            val thornsRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.CHITIN_THORNS)
            val steelApexThorns = if (EvolutionEngine.isApexSynergyActive(player, MutationCategory.IRON_CYBERNETICS)) 3 else 0
            val thornsDamage = when (thornsRank) {
                1 -> 3
                2 -> 5
                3 -> 8
                else -> 0
            } + steelApexThorns
            if (thornsDamage > 0 && dist <= 1) {
                enemy.currentHp -= thornsDamage
                if (enemy.isDead) {
                    enemyDied = true
                    tiles[enemy.pos.x][enemy.pos.y].hasBloodSplatter = true
                }
            }
        } else if (dist <= 6) {
            val dir = enemy.pos.directionTo(player.pos)
            val nextX = enemy.pos.x + dir.x
            val nextY = enemy.pos.y + dir.y
            val width = tiles.size
            val height = tiles[0].size

            if (nextX in 0 until width && nextY in 0 until height) {
                val isPropBlocking = props.any { !it.isDead && it.pos.x == nextX && it.pos.y == nextY && it.type.blocksMovement }
                if (tiles[nextX][nextY].isWalkable && !isPropBlocking && (nextX != player.pos.x || nextY != player.pos.y)) {
                    enemy.pos = GridPos(nextX, nextY)
                }
            }
        }

        val nextDist = enemy.pos.chebyshevDistance(player.pos)
        enemy.intent = if (nextDist <= enemy.archetype.range) {
            EnemyIntent(EnemyIntentType.ATTACK_MELEE, enemy.archetype.baseDamage, player.pos)
        } else {
            EnemyIntent(EnemyIntentType.APPROACH, 0, player.pos)
        }

        return EnemyTurnResult(damageToPlayer, enemyDied, attackFx, enemyAnimFx, emptyList())
    }

    fun tickStatusesAndEnvironment(
        player: Player,
        enemies: List<Enemy>,
        tiles: Array<Array<Tile>>,
        props: List<TacticalProp>
    ): List<String> {
        val logs = mutableListOf<String>()

        val thermalImmune = EvolutionEngine.getTraitRank(player, EvolutionTrait.THERMAL_IMMUNITY) > 0

        val playerStatusIter = player.statuses.iterator()
        while (playerStatusIter.hasNext()) {
            val status = playerStatusIter.next()
            when (status.type) {
                StatusEffectType.BURNING -> if (!thermalImmune) {
                    player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                }
                StatusEffectType.POISONED -> player.currentHp = (player.currentHp - 2).coerceAtLeast(0)
                StatusEffectType.BLEEDING -> player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                else -> {}
            }
        }
        player.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
        player.statuses.removeAll { it.durationTurns <= 0 }
        if (thermalImmune) {
            player.statuses.removeAll { it.type == StatusEffectType.BURNING }
        }

        // Titan Core passive turn regeneration
        val titanRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.TITAN_CORE)
        if (titanRank > 0 && player.currentHp > 0 && player.currentHp < player.maxHp) {
            val regen = titanRank * 2
            player.currentHp = (player.currentHp + regen).coerceAtMost(player.maxHp)
        }

        // Environmental tile interaction
        val pTile = tiles[player.pos.x][player.pos.y]
        when (pTile.type) {
            TileType.EMBER_HEARTH -> {
                if (!thermalImmune) {
                    player.currentHp = (player.currentHp - 4).coerceAtLeast(0)
                }
            }
            TileType.OIL_SLICK -> {
                if (thermalImmune) {
                    pTile.type = TileType.EMBER_HEARTH
                }
            }
            TileType.TOXIC_MIRE -> {
                player.statuses.add(StatusEffect(StatusEffectType.POISONED, 2))
            }
            else -> {}
        }

        // Toxic Step passive: chance to poison adjacent enemies at end of turn
        val toxicStepRank = EvolutionEngine.getTraitRank(player, EvolutionTrait.TOXIC_STEP)
        if (toxicStepRank > 0) {
            val poisonChance = 0.25f + (toxicStepRank * 0.20f)
            for (e in enemies.filter { !it.isDead && it.pos.chebyshevDistance(player.pos) <= 1 }) {
                if (Random.Default.nextFloat() < poisonChance) {
                    e.statuses.add(StatusEffect(StatusEffectType.POISONED, 2 + toxicStepRank))
                }
            }
        }

        // Stepped onto steam or spike trap
        val trap = props.find { !it.isDead && it.pos == player.pos }
        if (trap != null) {
            when (trap.type) {
                TacticalPropType.STEAM_PRESSURE_VENT -> {
                    if (!thermalImmune) {
                        player.currentHp = (player.currentHp - 6).coerceAtLeast(0)
                        player.statuses.add(StatusEffect(StatusEffectType.BURNING, 2))
                    }
                }
                TacticalPropType.SPIKE_PLATE -> {
                    player.currentHp = (player.currentHp - 5).coerceAtLeast(0)
                    player.statuses.add(StatusEffect(StatusEffectType.BLEEDING, 2))
                }
                else -> {}
            }
        }

        for (enemy in enemies.filter { !it.isDead }) {
            enemy.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
            enemy.statuses.removeAll { it.durationTurns <= 0 }
        }

        return logs
    }
}
