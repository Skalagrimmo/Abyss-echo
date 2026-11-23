package com.example.engine

import com.example.model.*

sealed class SlotIntegrationPreview {
    data class NewSlot(val slot: MutationSlot) : SlotIntegrationPreview()
    data class UpgradeRank(val slot: MutationSlot, val current: Mutation, val nextTier: Int) : SlotIntegrationPreview()
    data class ReplaceOrgan(val slot: MutationSlot, val existing: Mutation) : SlotIntegrationPreview()
    data class MaxRankConflict(val slot: MutationSlot, val existing: Mutation) : SlotIntegrationPreview()
}

enum class EvolutionActionType {
    INSTALLED_NEW,
    UPGRADED_RANK,
    REPLACED_EXISTING,
    MAX_RANK_REACHED,
    INSUFFICIENT_RESOURCES
}

data class EvolutionOutcome(
    val success: Boolean,
    val actionType: EvolutionActionType,
    val resultingMutation: Mutation? = null,
    val message: String,
    val unlockedSynergyTitle: String? = null
)

object EvolutionEngine {

    fun previewIntegration(player: Player, candidate: Mutation): SlotIntegrationPreview {
        val existing = player.activeMutations.find { it.slot == candidate.slot }
            ?: return SlotIntegrationPreview.NewSlot(candidate.slot)

        val isSameLineage = existing.id == candidate.id ||
            (existing.trait != EvolutionTrait.NONE && existing.trait == candidate.trait) ||
            (existing.name == candidate.name && existing.category == candidate.category)

        return if (isSameLineage) {
            if (existing.tier < 3) {
                SlotIntegrationPreview.UpgradeRank(candidate.slot, existing, existing.tier + 1)
            } else {
                SlotIntegrationPreview.MaxRankConflict(candidate.slot, existing)
            }
        } else {
            SlotIntegrationPreview.ReplaceOrgan(candidate.slot, existing)
        }
    }

    fun canAfford(player: Player, mutation: Mutation): Boolean {
        val sanityReq = mutation.sanityCost.coerceAtLeast(0)
        return player.biomass >= mutation.biomassCost &&
            player.scrap >= mutation.scrapCost &&
            player.sanity >= sanityReq
    }

    fun integrateMutation(
        player: Player,
        candidate: Mutation,
        deductCosts: Boolean = false
    ): EvolutionOutcome {
        if (deductCosts && !canAfford(player, candidate)) {
            return EvolutionOutcome(
                success = false,
                actionType = EvolutionActionType.INSUFFICIENT_RESOURCES,
                message = "Недостатньо біомаси, брухту або глузду."
            )
        }

        val prevSynergies = computeBranchSynergies(player)
        val preview = previewIntegration(player, candidate)

        if (preview is SlotIntegrationPreview.MaxRankConflict) {
            return EvolutionOutcome(
                success = false,
                actionType = EvolutionActionType.MAX_RANK_REACHED,
                resultingMutation = preview.existing,
                message = "Слот [${candidate.slot.title}] вже еволюціонував до III рангу."
            )
        }

        if (deductCosts) {
            player.biomass = (player.biomass - candidate.biomassCost).coerceAtLeast(0)
            player.scrap = (player.scrap - candidate.scrapCost).coerceAtLeast(0)
            if (candidate.sanityCost > 0) {
                player.sanity = (player.sanity - candidate.sanityCost).coerceAtLeast(0)
            } else if (candidate.sanityCost < 0) {
                player.sanity = (player.sanity - candidate.sanityCost).coerceAtMost(player.maxSanity)
            }
        }

        val resultMutation: Mutation
        val actionType: EvolutionActionType
        val actionMessage: String

        when (preview) {
            is SlotIntegrationPreview.NewSlot -> {
                resultMutation = candidate.copy(tier = 1, isAcquired = true)
                player.activeMutations.add(resultMutation)
                player.corruption = (player.corruption + candidate.corruptionIncrease).coerceIn(0, player.maxCorruption)
                actionType = EvolutionActionType.INSTALLED_NEW
                actionMessage = "Прищеплено: ${resultMutation.name} [I]"
            }
            is SlotIntegrationPreview.UpgradeRank -> {
                val idx = player.activeMutations.indexOfFirst { it.slot == candidate.slot }
                val existing = preview.current
                resultMutation = existing.copy(
                    tier = preview.nextTier,
                    isAcquired = true,
                    grantedSkillId = existing.grantedSkillId ?: candidate.grantedSkillId,
                    bonusMaxHp = maxOf(existing.bonusMaxHp, candidate.bonusMaxHp),
                    bonusArmor = maxOf(existing.bonusArmor, candidate.bonusArmor),
                    bonusAttackPower = maxOf(existing.bonusAttackPower, candidate.bonusAttackPower),
                    bonusAp = maxOf(existing.bonusAp, candidate.bonusAp),
                    bonusFov = maxOf(existing.bonusFov, candidate.bonusFov)
                )
                if (idx >= 0) {
                    player.activeMutations[idx] = resultMutation
                }
                val extraCorruption = (candidate.corruptionIncrease / 2).coerceAtLeast(1)
                player.corruption = (player.corruption + extraCorruption).coerceIn(0, player.maxCorruption)
                actionType = EvolutionActionType.UPGRADED_RANK
                actionMessage = "Еволюція слота: ${resultMutation.name} [${resultMutation.tierRoman()}]"
            }
            is SlotIntegrationPreview.ReplaceOrgan -> {
                player.activeMutations.removeAll { it.slot == candidate.slot }
                resultMutation = candidate.copy(tier = 1, isAcquired = true)
                player.activeMutations.add(resultMutation)
                val netCorruption = (candidate.corruptionIncrease - preview.existing.corruptionIncrease / 3).coerceAtLeast(2)
                player.corruption = (player.corruption + netCorruption).coerceIn(0, player.maxCorruption)
                actionType = EvolutionActionType.REPLACED_EXISTING
                actionMessage = "Замінено ${preview.existing.name} → ${resultMutation.name} [I]"
            }
            is SlotIntegrationPreview.MaxRankConflict -> {
                return EvolutionOutcome(false, EvolutionActionType.MAX_RANK_REACHED, preview.existing, "Макс. ранг")
            }
        }

        // Unlock skill if present
        val skill = resolveSkillById(resultMutation.grantedSkillId)
        if (skill != null && player.knownSkills.none { it.id == skill.id }) {
            player.knownSkills.add(skill)
        }

        recalculatePlayerStats(player)

        val newSynergies = computeBranchSynergies(player)
        val unlocked = findNewlyUnlockedSynergy(prevSynergies, newSynergies)

        return EvolutionOutcome(
            success = true,
            actionType = actionType,
            resultingMutation = resultMutation,
            message = actionMessage,
            unlockedSynergyTitle = unlocked
        )
    }

    fun convertHarvestOptionToMutation(
        harvest: DefeatedHarvest,
        option: HarvestMutationOption
    ): Mutation = Mutation(
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
        isAcquired = true,
        trait = option.trait,
        sourceArchetype = harvest.enemyArchetype
    )

    fun storeHarvestedOrganInBioBank(
        player: Player,
        harvest: DefeatedHarvest,
        option: HarvestMutationOption
    ): HarvestedOrgan {
        val isCyber = option.category == MutationCategory.IRON_CYBERNETICS
        val organ = HarvestedOrgan(
            id = "bio_${System.currentTimeMillis()}_${player.harvestedOrgans.size}",
            name = option.title,
            subtitle = option.subtitle,
            enemyTitle = harvest.enemyTitle,
            sourceArchetype = harvest.enemyArchetype,
            slot = option.slot,
            category = option.category,
            trait = option.trait,
            bonusMaxHp = option.bonusMaxHp,
            bonusArmor = option.bonusArmor,
            bonusAttackPower = option.bonusAttackPower,
            bonusAp = option.bonusAp,
            bonusFov = option.bonusFov,
            corruptionDelta = option.corruptionDelta,
            sanityDelta = option.sanityDelta,
            grantedSkill = option.grantedSkill,
            passiveTrait = option.passiveTrait,
            salvageBiomass = if (isCyber) 4 else 12,
            salvageScrap = if (isCyber) 14 else 4
        )
        player.harvestedOrgans.add(organ)
        return organ
    }

    fun integrateBioBankOrgan(player: Player, organId: String): EvolutionOutcome {
        val organ = player.harvestedOrgans.find { it.id == organId }
            ?: return EvolutionOutcome(false, EvolutionActionType.INSUFFICIENT_RESOURCES, message = "Орган не знайдено.")
        val outcome = integrateMutation(player, organ.toMutation(), deductCosts = false)
        if (outcome.success) {
            player.harvestedOrgans.removeAll { it.id == organId }
            if (organ.sanityDelta != 0) {
                player.sanity = (player.sanity + organ.sanityDelta).coerceIn(0, player.maxSanity)
            }
        }
        return outcome
    }

    fun salvageBioBankOrgan(player: Player, organId: String): Pair<Int, Int> {
        val organ = player.harvestedOrgans.find { it.id == organId } ?: return 0 to 0
        player.harvestedOrgans.removeAll { it.id == organId }
        player.biomass += organ.salvageBiomass
        player.scrap += organ.salvageScrap
        return organ.salvageBiomass to organ.salvageScrap
    }

    fun removeEquippedMutation(player: Player, slot: MutationSlot): Pair<Int, Int> {
        val existing = player.activeMutations.find { it.slot == slot } ?: return 0 to 0
        player.activeMutations.removeAll { it.slot == slot }
        val refundBio = (6 * existing.tier)
        val refundScrap = (6 * existing.tier)
        player.biomass += refundBio
        player.scrap += refundScrap
        recalculatePlayerStats(player)
        return refundBio to refundScrap
    }

    fun recalculatePlayerStats(player: Player) {
        val oldMaxHp = player.maxHp
        val synergies = computeBranchSynergies(player)
        val occultMinorFov = if (synergies.any { it.category == MutationCategory.OCCULT_SIGIL && it.minorActive }) 2 else 0

        val newMaxHp = (player.baseMaxHp + player.activeMutations.sumOf { it.effectiveMaxHp() }).coerceAtLeast(20)
        val hpDelta = newMaxHp - oldMaxHp

        player.maxHp = newMaxHp
        if (hpDelta > 0) {
            player.currentHp = (player.currentHp + hpDelta).coerceAtMost(newMaxHp)
        } else if (player.currentHp > newMaxHp) {
            player.currentHp = newMaxHp.coerceAtLeast(1)
        }

        player.armor = player.baseArmor
        player.maxAp = (player.baseMaxAp + player.activeMutations.sumOf { it.effectiveBonusAp() }).coerceAtLeast(2)
        player.currentAp = player.currentAp.coerceAtMost(player.maxAp + 2)
        player.fovRadius = (player.baseFovRadius + player.activeMutations.sumOf { it.effectiveFov() } + occultMinorFov).coerceAtLeast(3)
    }

    fun computeBranchSynergies(player: Player): List<BranchSynergy> {
        val fleshCount = player.activeMutations.count { it.category == MutationCategory.CHTHONIC_FLESH }
        val steelCount = player.activeMutations.count { it.category == MutationCategory.IRON_CYBERNETICS }
        val occultCount = player.activeMutations.count { it.category == MutationCategory.OCCULT_SIGIL }

        return listOf(
            BranchSynergy(
                category = MutationCategory.CHTHONIC_FLESH,
                equippedCount = fleshCount,
                minorActive = fleshCount >= 2,
                apexActive = fleshCount >= 3,
                title = "Симбіоз Плоті",
                minorDesc = "2+: +12% шансу криту та +25% ефекту зцілення.",
                apexDesc = "3+: +4 ОЗ за вбивство та отруєння при кожному криті."
            ),
            BranchSynergy(
                category = MutationCategory.IRON_CYBERNETICS,
                equippedCount = steelCount,
                minorActive = steelCount >= 2,
                apexActive = steelCount >= 3,
                title = "Протокол Сталі",
                minorDesc = "2+: -2 до вхідного фізичного урону.",
                apexDesc = "3+: +3 шипи броні та +1 ОД щодругий хід."
            ),
            BranchSynergy(
                category = MutationCategory.OCCULT_SIGIL,
                equippedCount = occultCount,
                minorActive = occultCount >= 2,
                apexActive = occultCount >= 3,
                title = "Резонанс Гліфів",
                minorDesc = "2+: +15% шкоди навичками та +2 Огляду.",
                apexDesc = "3+: +20% до шкоди та +3 Глузду за критичний удар."
            )
        )
    }

    fun getTraitRank(player: Player, trait: EvolutionTrait): Int {
        val equippedRank = player.activeMutations
            .filter { it.trait == trait }
            .maxOfOrNull { it.tier.coerceIn(1, 3) } ?: 0

        if (equippedRank > 0) return equippedRank

        // Apex synergies can also unlock base rank-1 passive effects
        val synergies = computeBranchSynergies(player)
        if (trait == EvolutionTrait.TOXIC_CRIT &&
            synergies.any { it.category == MutationCategory.CHTHONIC_FLESH && it.apexActive }
        ) {
            return 1
        }
        return 0
    }

    fun isMinorSynergyActive(player: Player, category: MutationCategory): Boolean =
        player.activeMutations.count { it.category == category } >= 2

    fun isApexSynergyActive(player: Player, category: MutationCategory): Boolean =
        player.activeMutations.count { it.category == category } >= 3

    fun resolveSkillById(skillId: String?): CombatSkill? = when (skillId) {
        "skill_siphon_lash" -> SkillCatalog.siphonLash
        "skill_piston_slam" -> SkillCatalog.pistonSlam
        "skill_steam_vent" -> SkillCatalog.steamVent
        "skill_blood_lance" -> SkillCatalog.bloodLance
        "skill_void_gaze" -> SkillCatalog.voidGaze
        else -> null
    }

    private fun findNewlyUnlockedSynergy(
        before: List<BranchSynergy>,
        after: List<BranchSynergy>
    ): String? {
        for (i in after.indices) {
            val prev = before.getOrNull(i) ?: continue
            val next = after[i]
            if (!prev.apexActive && next.apexActive) {
                return "${next.title} (АПЕКС 3/5)"
            }
            if (!prev.minorActive && next.minorActive) {
                return "${next.title} (2/5)"
            }
        }
        return null
    }
}
