package com.example.engine

import com.example.model.*
import kotlin.random.Random

data class CombatResult(
    val damageDealt: Int,
    val isCritical: Boolean,
    val enemyDied: Boolean,
    val comboAdvancedTo: ComboStage,
    val playerHealed: Int = 0,
    val statusInflicted: StatusEffectType? = null,
    val logs: List<String>
)

data class EnemyTurnResult(
    val damageDealtToPlayer: Int = 0,
    val enemyDiedFromThorns: Boolean = false,
    val logs: List<String>
)

object CombatEngine {

    fun executePlayerAttack(
        player: Player,
        enemy: Enemy,
        skill: CombatSkill,
        tiles: Array<Array<Tile>>
    ): CombatResult {
        val logs = mutableListOf<String>()
        val random = Random.Default
        var healed = 0

        // Cost AP
        player.currentAp = (player.currentAp - skill.apCost).coerceAtLeast(0)

        // Self HP cost (e.g. blood lance)
        if (skill.selfHpCost > 0) {
            player.currentHp = (player.currentHp - skill.selfHpCost).coerceAtLeast(1)
        }

        // Combo calculation
        var comboMultiplier = 1.0f
        var isCrit = random.nextFloat() < 0.15f
        if (player.sanity < 30) isCrit = isCrit || (random.nextFloat() < 0.25f) // Desperation frenzy

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

        // Calculate total damage
        val baseAtk = player.totalAttackPower()
        var rawDmg = ((skill.baseDamage + (baseAtk / 2)) * comboMultiplier).toInt()
        if (isCrit) rawDmg = (rawDmg * 1.5f).toInt()

        // Enemy armor mitigation
        val netDmg = (rawDmg - enemy.armor).coerceAtLeast(2)
        enemy.currentHp -= netDmg

        // Knockback into wall (16-bit systemic mechanic)
        if (skill.pushesTarget) {
            val dir = player.pos.directionTo(enemy.pos)
            val pushTargetPos = GridPos(enemy.pos.x + dir.x, enemy.pos.y + dir.y)
            val width = tiles.size
            val height = tiles[0].size

            if (pushTargetPos.x in 0 until width && pushTargetPos.y in 0 until height) {
                val targetTile = tiles[pushTargetPos.x][pushTargetPos.y]
                if (!targetTile.isWalkable) {
                    val wallDmg = 8
                    enemy.currentHp -= wallDmg
                    enemy.statuses.add(StatusEffect(StatusEffectType.STUNNED, 1))
                } else {
                    enemy.pos = pushTargetPos
                }
            }
        }

        // Apply skill status
        if (skill.appliesStatus != null) {
            enemy.statuses.add(StatusEffect(skill.appliesStatus, skill.statusTurns))
        }

        // Self heal (Siphon tendrils)
        if (skill.healsSelf > 0) {
            player.currentHp = (player.currentHp + skill.healsSelf).coerceAtMost(player.maxHp)
            healed += skill.healsSelf
        }

        // Passive from mutations
        if (player.activeMutations.any { it.id == "mut_siphon_tendrils" } && isCrit) {
            player.currentHp = (player.currentHp + 4).coerceAtMost(player.maxHp)
            healed += 4
        }

        val enemyDied = enemy.isDead
        if (enemyDied) {
            player.scrap += enemy.archetype.scrapReward
            player.biomass += enemy.archetype.biomassReward
            tiles[enemy.pos.x][enemy.pos.y].hasBloodSplatter = true

            if (player.activeMutations.any { it.id == "mut_soul_cage" }) {
                player.sanity = (player.sanity + 3).coerceAtMost(player.maxSanity)
            }
        }

        return CombatResult(
            damageDealt = netDmg,
            isCritical = isCrit,
            enemyDied = enemyDied,
            comboAdvancedTo = nextStage,
            playerHealed = healed,
            statusInflicted = skill.appliesStatus,
            logs = logs
        )
    }

    fun processEnemyTurn(
        enemy: Enemy,
        player: Player,
        tiles: Array<Array<Tile>>
    ): EnemyTurnResult {
        if (enemy.isDead) return EnemyTurnResult(0, false, emptyList())

        // Check stun
        val stun = enemy.statuses.find { it.type == StatusEffectType.STUNNED }
        if (stun != null) {
            return EnemyTurnResult(0, false, emptyList())
        }

        val dist = enemy.pos.chebyshevDistance(player.pos)
        var damageToPlayer = 0
        var enemyDied = false

        if (dist <= enemy.archetype.range) {
            val rawDamage = enemy.archetype.baseDamage
            val defense = player.totalArmor()
            damageToPlayer = (rawDamage - defense).coerceAtLeast(1)

            player.currentHp = (player.currentHp - damageToPlayer).coerceAtLeast(0)

            // Thorns from chitin carapace
            if (player.activeMutations.any { it.id == "mut_chitin_carapace" }) {
                enemy.currentHp -= 3
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
                if (tiles[nextX][nextY].isWalkable && (nextX != player.pos.x || nextY != player.pos.y)) {
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

        return EnemyTurnResult(damageToPlayer, enemyDied, emptyList())
    }

    fun tickStatusesAndEnvironment(
        player: Player,
        enemies: List<Enemy>,
        tiles: Array<Array<Tile>>
    ): List<String> {
        val logs = mutableListOf<String>()

        val playerStatusIter = player.statuses.iterator()
        while (playerStatusIter.hasNext()) {
            val status = playerStatusIter.next()
            when (status.type) {
                StatusEffectType.BURNING -> player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                StatusEffectType.POISONED -> player.currentHp = (player.currentHp - 2).coerceAtLeast(0)
                StatusEffectType.BLEEDING -> player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                else -> {}
            }
        }
        player.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
        player.statuses.removeAll { it.durationTurns <= 0 }

        val pTile = tiles[player.pos.x][player.pos.y]
        when (pTile.type) {
            TileType.EMBER_HEARTH -> {
                if (!player.activeMutations.any { it.id == "mut_smelter_core" }) {
                    player.currentHp = (player.currentHp - 4).coerceAtLeast(0)
                }
            }
            TileType.TOXIC_MIRE -> {
                player.statuses.add(StatusEffect(StatusEffectType.POISONED, 2))
            }
            else -> {}
        }

        for (enemy in enemies.filter { !it.isDead }) {
            enemy.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
            enemy.statuses.removeAll { it.durationTurns <= 0 }
        }

        return logs
    }
}
