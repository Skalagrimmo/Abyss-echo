package com.example.engine

import com.example.model.*
import kotlin.random.Random

data class CombatResult(
    val damageDealt: Int,
    val isCritical: Boolean,
    val enemyDied: Boolean,
    val comboAdvancedTo: ComboStage,
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

        // Cost AP
        player.currentAp = (player.currentAp - skill.apCost).coerceAtLeast(0)

        // Self HP cost (e.g. blood lance)
        if (skill.selfHpCost > 0) {
            player.currentHp = (player.currentHp - skill.selfHpCost).coerceAtLeast(1)
            logs.add("🩸 Ви пожертвували ${skill.selfHpCost} ОЗ для вивільнення окультної сили!")
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
                        logs.add("⚔️ [КОМБО 1/3] Швидкий випад! Натиск розпочато.")
                        ComboStage.LIGHT_1
                    }
                    ComboStage.LIGHT_1 -> {
                        player.comboHits = 2
                        comboMultiplier = 1.25f
                        logs.add("⚔️ [КОМБО 2/3] Повторний розсікаючий удар! Ворог відкритий.")
                        ComboStage.LIGHT_2
                    }
                    ComboStage.LIGHT_2 -> {
                        player.comboHits = 3
                        comboMultiplier = 1.5f
                        logs.add("⚡ [КОМБО ГОТОВЕ!] Позиція ідеальна для важкого розколу!")
                        ComboStage.HEAVY_PRIMED
                    }
                    ComboStage.HEAVY_PRIMED -> ComboStage.HEAVY_PRIMED
                }
            }
            SkillCatalog.strikeHeavy.id -> {
                if (player.comboStage == ComboStage.HEAVY_PRIMED || player.comboHits >= 2) {
                    comboMultiplier = 2.0f
                    isCrit = true
                    logs.add("💥 [ФІНІШЕР РУЙНУВАННЯ!] Важкий розкол зносить захист!")
                } else {
                    logs.add("🔨 Важкий удар по броні ворога.")
                }
                player.comboHits = 0
                ComboStage.READY
            }
            else -> {
                // Mutated skills might reset or maintain combo
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
        logs.add("⚔️ Ви завдали $netDmg урону ${enemy.archetype.title} (ОЗ: ${enemy.currentHp.coerceAtLeast(0)}/${enemy.maxHp})${if (isCrit) " [КРИТИЧНО!]" else ""}")

        // Knockback into wall (Section 12 & 13)
        if (skill.pushesTarget) {
            val dir = player.pos.directionTo(enemy.pos)
            val pushTargetPos = GridPos(enemy.pos.x + dir.x, enemy.pos.y + dir.y)
            val width = tiles.size
            val height = tiles[0].size

            if (pushTargetPos.x in 0 until width && pushTargetPos.y in 0 until height) {
                val targetTile = tiles[pushTargetPos.x][pushTargetPos.y]
                if (!targetTile.isWalkable) {
                    // Smashed into wall!
                    val wallDmg = 8
                    enemy.currentHp -= wallDmg
                    enemy.statuses.add(StatusEffect(StatusEffectType.STUNNED, 1))
                    logs.add("🧱 Ворога з силою вбито в кам'яну стіну! +$wallDmg урону та ОГЛУШЕННЯ!")
                } else {
                    enemy.pos = pushTargetPos
                    logs.add("💨 Ворога відкинуто назад!")
                }
            }
        }

        // Apply skill status
        if (skill.appliesStatus != null) {
            enemy.statuses.add(StatusEffect(skill.appliesStatus, skill.statusTurns))
            logs.add("✨ Накладено ефект [${skill.appliesStatus.title}] на ${skill.statusTurns} ходи.")
        }

        // Self heal (Siphon tendrils)
        if (skill.healsSelf > 0) {
            player.currentHp = (player.currentHp + skill.healsSelf).coerceAtMost(player.maxHp)
            logs.add("🩸 Хтонічні щупальця випили життєву силу! +${skill.healsSelf} ОЗ відновлено.")
        }

        // Passive from mutations
        if (player.activeMutations.any { it.id == "mut_siphon_tendrils" } && isCrit) {
            player.currentHp = (player.currentHp + 4).coerceAtMost(player.maxHp)
            logs.add("🧬 Мутація Щупалець повернула +4 ОЗ від критичного удару.")
        }

        // Check if enemy died
        val enemyDied = enemy.isDead
        if (enemyDied) {
            logs.add("💀 ${enemy.archetype.title} гине у конвульсіях!")
            player.scrap += enemy.archetype.scrapReward
            player.biomass += enemy.archetype.biomassReward
            logs.add("📦 Здобуто: +${enemy.archetype.scrapReward} Брухту, +${enemy.archetype.biomassReward} Біомаси.")

            // Mark blood splatter on tile
            tiles[enemy.pos.x][enemy.pos.y].hasBloodSplatter = true

            // Soul cage mutation passive
            if (player.activeMutations.any { it.id == "mut_soul_cage" }) {
                player.sanity = (player.sanity + 3).coerceAtMost(player.maxSanity)
                logs.add("🔮 Клітка Душ поглинула пам'ять ворога (+3 Глузду).")
            }
        }

        return CombatResult(
            damageDealt = netDmg,
            isCritical = isCrit,
            enemyDied = enemyDied,
            comboAdvancedTo = nextStage,
            logs = logs
        )
    }

    fun processEnemyTurn(
        enemy: Enemy,
        player: Player,
        tiles: Array<Array<Tile>>
    ): List<String> {
        val logs = mutableListOf<String>()
        if (enemy.isDead) return logs

        // Check stun
        val stun = enemy.statuses.find { it.type == StatusEffectType.STUNNED }
        if (stun != null) {
            logs.add("💫 ${enemy.archetype.title} оглушений і пропускає свій хід!")
            return logs
        }

        val dist = enemy.pos.chebyshevDistance(player.pos)

        // If adjacent, attack melee
        if (dist <= enemy.archetype.range) {
            val rawDamage = enemy.archetype.baseDamage
            val defense = player.totalArmor()
            val finalDamage = (rawDamage - defense).coerceAtLeast(1)

            player.currentHp = (player.currentHp - finalDamage).coerceAtLeast(0)
            logs.add("🩸 ${enemy.archetype.title} атакує вас на $finalDamage урону! (Залишилось ОЗ: ${player.currentHp})")

            // Thorns from chitin carapace mutation
            if (player.activeMutations.any { it.id == "mut_chitin_carapace" }) {
                enemy.currentHp -= 3
                logs.add("🛡️ Хітиновий панцир завдає 3 шипового урону нападнику!")
            }
        } else if (dist <= 6) {
            // Pathfind towards player
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

        // Set next intent for strategic visibility
        val nextDist = enemy.pos.chebyshevDistance(player.pos)
        enemy.intent = if (nextDist <= enemy.archetype.range) {
            EnemyIntent(EnemyIntentType.ATTACK_MELEE, enemy.archetype.baseDamage, player.pos)
        } else {
            EnemyIntent(EnemyIntentType.APPROACH, 0, player.pos)
        }

        return logs
    }

    fun tickStatusesAndEnvironment(
        player: Player,
        enemies: List<Enemy>,
        tiles: Array<Array<Tile>>
    ): List<String> {
        val logs = mutableListOf<String>()

        // Player status ticks
        val playerStatusIter = player.statuses.iterator()
        while (playerStatusIter.hasNext()) {
            val status = playerStatusIter.next()
            when (status.type) {
                StatusEffectType.BURNING -> {
                    player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                    logs.add("🔥 Ви горите! -3 ОЗ від полум'я.")
                }
                StatusEffectType.POISONED -> {
                    player.currentHp = (player.currentHp - 2).coerceAtLeast(0)
                    logs.add("🧪 Токсична отрута роз'їдає плоть! -2 ОЗ.")
                }
                StatusEffectType.BLEEDING -> {
                    player.currentHp = (player.currentHp - 3).coerceAtLeast(0)
                    logs.add("🩸 Рани кровоточать! -3 ОЗ.")
                }
                else -> {}
            }
        }
        player.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
        player.statuses.removeAll { it.durationTurns <= 0 }

        // Environmental tile check for player
        val pTile = tiles[player.pos.x][player.pos.y]
        when (pTile.type) {
            TileType.EMBER_HEARTH -> {
                if (!player.activeMutations.any { it.id == "mut_smelter_core" }) {
                    player.currentHp = (player.currentHp - 4).coerceAtLeast(0)
                    logs.add("🔥 Ви наступили на розпечений шлак! -4 ОЗ.")
                }
            }
            TileType.TOXIC_MIRE -> {
                player.statuses.add(StatusEffect(StatusEffectType.POISONED, 2))
                logs.add("🧪 Ви стоїте у токсичному слизу! Накладено отруєння.")
            }
            else -> {}
        }

        // Enemy status ticks
        for (enemy in enemies.filter { !it.isDead }) {
            enemy.statuses.replaceAll { it.copy(durationTurns = it.durationTurns - 1) }
            enemy.statuses.removeAll { it.durationTurns <= 0 }
        }

        return logs
    }
}
