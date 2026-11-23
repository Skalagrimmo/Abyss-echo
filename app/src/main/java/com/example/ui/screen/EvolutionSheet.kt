package com.example.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EvolutionEngine
import com.example.engine.SlotIntegrationPreview
import com.example.model.*
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*

private enum class EvolutionTab {
    BIO_BANK,
    LABORATORY
}

@Composable
fun EvolutionSheet(
    player: Player,
    onAcquireMutation: (Mutation) -> Unit,
    activeSlotPulse: SlotIntegrationPulseFx? = null,
    onIntegrateBioBank: (String) -> Unit = {},
    onSalvageBioBank: (String) -> Unit = {},
    onRemoveSlot: (MutationSlot) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var activeTab by remember {
        mutableStateOf(
            if (player.harvestedOrgans.isNotEmpty()) EvolutionTab.BIO_BANK else EvolutionTab.LABORATORY
        )
    }
    var selectedCategory by remember { mutableStateOf<MutationCategory?>(null) }
    var inspectedSlot by remember { mutableStateOf<MutationSlot?>(null) }

    val synergies = remember(player.activeMutations.toList()) {
        EvolutionEngine.computeBranchSynergies(player)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🧬 ЛАБОРАТОРІЯ ЕВОЛЮЦІЇ",
                color = EldritchViolet,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            RetroButton(
                text = "НАЗАД",
                onClick = onBack,
                color = SurfaceVariantIron,
                textColor = BoneIvory,
                testTag = "btn_back_evolution"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Resources & Vitals Strip
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderMetal,
            backgroundColor = SurfaceIron
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ResourceMetric("БІОМАСА", "${player.biomass}", ToxicGreen)
                ResourceMetric("БРУХТ", "${player.scrap}", ForgeAmber)
                ResourceMetric("ГЛУЗД", "${player.sanity}", SanityCyan)
                ResourceMetric("СПОТВОР.", "${player.corruption}%", EldritchViolet)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("❤️ ${player.maxHp}", color = BloodCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("🛡️ ${player.totalArmor()}", color = SanityCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("⚔️ ${player.totalAttackPower()}", color = ForgeAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("⚡ ${player.maxAp} ОД", color = ToxicGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("👁️ ${player.fovRadius}", color = BoneIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Anatomical Slots (5 Slots with Tier I-III & Interactive Inspector)
        Text(
            text = "АНАТОМІЧНІ СЛОТИ (${player.activeMutations.size}/5) — НАТИСНІТЬ ДЛЯ ОГЛЯДУ:",
            color = AshGrey,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MutationSlot.entries.forEach { slot ->
                val activeInSlot = player.activeMutations.find { it.slot == slot }
                AnimatedAnatomicalSlotCard(
                    slot = slot,
                    activeMutation = activeInSlot,
                    isInspected = inspectedSlot == slot,
                    pulseFx = activeSlotPulse,
                    onClick = {
                        inspectedSlot = if (inspectedSlot == slot) null else slot
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Expanded Slot Inspector
        val inspectedMutation = inspectedSlot?.let { s -> player.activeMutations.find { it.slot == s } }
        AnimatedVisibility(visible = inspectedMutation != null) {
            if (inspectedMutation != null) {
                Spacer(modifier = Modifier.height(6.dp))
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = EldritchViolet,
                    backgroundColor = SurfaceVariantIron
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${inspectedMutation.name} [Ранг ${inspectedMutation.tierRoman()}]",
                                color = BoneIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "✨ ${inspectedMutation.passiveDescription ?: inspectedMutation.trait.passiveSummary}",
                                color = ForgeAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = buildScaledStatsSummary(inspectedMutation),
                                color = SanityCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RetroButton(
                            text = "ВИЛУЧИТИ",
                            onClick = {
                                onRemoveSlot(inspectedMutation.slot)
                                inspectedSlot = null
                            },
                            color = BloodCrimson,
                            textColor = BoneIvory,
                            testTag = "btn_remove_slot"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Branch Synergies Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            synergies.forEach { syn ->
                val synColor = when (syn.category) {
                    MutationCategory.CHTHONIC_FLESH -> ToxicGreen
                    MutationCategory.IRON_CYBERNETICS -> ForgeAmber
                    MutationCategory.OCCULT_SIGIL -> EldritchViolet
                }
                val isActive = syn.minorActive || syn.apexActive
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CutCornerShape(4.dp))
                        .background(if (isActive) synColor.copy(alpha = 0.14f) else AbyssalDark)
                        .border(
                            1.dp,
                            if (syn.apexActive) synColor else if (syn.minorActive) synColor.copy(alpha = 0.7f) else BorderMetal,
                            CutCornerShape(4.dp)
                        )
                        .padding(6.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = syn.title.substringBefore(" "),
                                color = if (isActive) synColor else AshGrey,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${syn.equippedCount}/3",
                                color = if (syn.apexActive) ForgeAmber else if (syn.minorActive) synColor else AshGrey,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = when {
                                syn.apexActive -> "АПЕКС: ${syn.apexDesc.substringAfter(": ")}"
                                syn.minorActive -> "АКТ: ${syn.minorDesc.substringAfter(": ")}"
                                else -> "2+: ${syn.minorDesc.substringAfter(": ")}"
                            },
                            color = if (isActive) BoneIvory else AshGrey.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Mode Tabs: BIO_BANK vs LABORATORY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_bio_bank")
                    .clip(CutCornerShape(4.dp))
                    .background(if (activeTab == EvolutionTab.BIO_BANK) SanityCyan else SurfaceVariantIron)
                    .border(1.5.dp, if (activeTab == EvolutionTab.BIO_BANK) BoneIvory else BorderMetal, CutCornerShape(4.dp))
                    .clickable { activeTab = EvolutionTab.BIO_BANK }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧫 БІО-БАНК (${player.harvestedOrgans.size})",
                    color = if (activeTab == EvolutionTab.BIO_BANK) VoidBlack else BoneIvory,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_laboratory")
                    .clip(CutCornerShape(4.dp))
                    .background(if (activeTab == EvolutionTab.LABORATORY) ForgeAmber else SurfaceVariantIron)
                    .border(1.5.dp, if (activeTab == EvolutionTab.LABORATORY) BoneIvory else BorderMetal, CutCornerShape(4.dp))
                    .clickable { activeTab = EvolutionTab.LABORATORY }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧬 ЛАБОРАТОРІЯ",
                    color = if (activeTab == EvolutionTab.LABORATORY) VoidBlack else BoneIvory,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeTab == EvolutionTab.BIO_BANK) {
            if (player.harvestedOrgans.isEmpty()) {
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderMetal,
                    backgroundColor = AbyssalDark
                ) {
                    Text(
                        text = "🧫 БІО-БАНК ПОРОЖНІЙ",
                        color = SanityCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Перемагайте ворогів у підземеллі та зберігайте їхні органи в Біо-банк, щоб пізніше інтегрувати у вільні слоти або еволюціонувати білд до III рангу.",
                        color = AshGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(player.harvestedOrgans, key = { it.id }) { organ ->
                        val preview = EvolutionEngine.previewIntegration(player, organ.toMutation())
                        val branchColor = when (organ.category) {
                            MutationCategory.CHTHONIC_FLESH -> ToxicGreen
                            MutationCategory.IRON_CYBERNETICS -> ForgeAmber
                            MutationCategory.OCCULT_SIGIL -> EldritchViolet
                        }
                        val (actionText, actionColor, canIntegrate) = when (preview) {
                            is SlotIntegrationPreview.NewSlot -> Triple("ІНТЕГРУВАТИ [I]", branchColor, true)
                            is SlotIntegrationPreview.UpgradeRank -> Triple("ЕВОЛЮЦІЯ [${preview.nextTier}]", SanityCyan, true)
                            is SlotIntegrationPreview.ReplaceOrgan -> Triple("ЗАМІНИТИ", ForgeAmber, true)
                            is SlotIntegrationPreview.MaxRankConflict -> Triple("МАКС. III", AshGrey, false)
                        }

                        PixelCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = branchColor,
                            backgroundColor = SurfaceIron
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = organ.name,
                                    color = BoneIvory,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "[${organ.slot.title.substringBefore(" /")}]",
                                    color = branchColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "Джерело: ${organ.enemyTitle} • ${organ.category.title}",
                                color = AshGrey,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = organ.subtitle,
                                color = BoneIvory.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RetroButton(
                                    text = actionText,
                                    onClick = { onIntegrateBioBank(organ.id) },
                                    enabled = canIntegrate,
                                    color = actionColor,
                                    textColor = VoidBlack,
                                    modifier = Modifier.weight(1f),
                                    testTag = "btn_biobank_equip_${organ.id}"
                                )
                                RetroButton(
                                    text = "♻️ +${organ.salvageBiomass}Б/+${organ.salvageScrap}М",
                                    onClick = { onSalvageBioBank(organ.id) },
                                    color = SurfaceVariantIron,
                                    textColor = ToxicGreen,
                                    modifier = Modifier.weight(1f),
                                    testTag = "btn_biobank_salvage_${organ.id}"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Category Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val allCats = listOf<MutationCategory?>(null) + MutationCategory.entries
                allCats.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(CutCornerShape(4.dp))
                            .background(if (isSelected) ForgeAmber else SurfaceVariantIron)
                            .border(1.dp, if (isSelected) BoneIvory else BorderMetal, CutCornerShape(4.dp))
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat?.title?.take(8) ?: "Всі",
                            color = if (isSelected) VoidBlack else BoneIvory,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedMutations = MutationCatalog.allMutations.filter {
                selectedCategory == null || it.category == selectedCategory
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedMutations, key = { it.id }) { mutation ->
                    val preview = EvolutionEngine.previewIntegration(player, mutation)
                    val canAfford = EvolutionEngine.canAfford(player, mutation)
                    val isMaxed = preview is SlotIntegrationPreview.MaxRankConflict

                    val (btnText, btnColor) = when (preview) {
                        is SlotIntegrationPreview.NewSlot -> "ПРИЩЕПИТИ [I]" to ForgeAmber
                        is SlotIntegrationPreview.UpgradeRank -> "ЕВОЛЮЦІЯ [${preview.nextTier}]" to SanityCyan
                        is SlotIntegrationPreview.ReplaceOrgan -> "ЗАМІНИТИ" to BloodCrimson
                        is SlotIntegrationPreview.MaxRankConflict -> "РАНГ III" to AshGrey
                    }

                    PixelCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = when {
                            isMaxed -> EldritchViolet
                            preview is SlotIntegrationPreview.UpgradeRank -> SanityCyan
                            canAfford -> ForgeAmber
                            else -> BorderMetal
                        },
                        backgroundColor = if (isMaxed) SurfaceVariantIron else SurfaceIron
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mutation.name,
                                        color = BoneIvory,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[${mutation.slot.title.substringBefore(" /")}]",
                                        color = AshGrey,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (preview is SlotIntegrationPreview.ReplaceOrgan) {
                                    Text(
                                        text = "Замінить: ${preview.existing.name}",
                                        color = BloodCrimson,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mutation.description,
                                    color = AshGrey,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (mutation.passiveDescription != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "✨ Ефект: ${mutation.passiveDescription}",
                                        color = ForgeAmber,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (mutation.biomassCost > 0) {
                                        Text("Біо: ${mutation.biomassCost}", color = ToxicGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    if (mutation.scrapCost > 0) {
                                        Text("Брухт: ${mutation.scrapCost}", color = ForgeAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    if (mutation.sanityCost > 0) {
                                        Text("Глузд: -${mutation.sanityCost}", color = SanityCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Text("Спотв: +${mutation.corruptionIncrease}%", color = EldritchViolet, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            RetroButton(
                                text = btnText,
                                onClick = { onAcquireMutation(mutation) },
                                enabled = canAfford && !isMaxed,
                                color = btnColor,
                                textColor = if (btnColor == BloodCrimson) BoneIvory else VoidBlack,
                                testTag = "btn_mutate_${mutation.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = BoneIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}


private fun buildScaledStatsSummary(m: Mutation): String {
    val parts = mutableListOf<String>()
    if (m.effectiveMaxHp() > 0) parts.add("+${m.effectiveMaxHp()} ОЗ")
    if (m.effectiveArmor() > 0) parts.add("+${m.effectiveArmor()} Броня")
    if (m.effectiveAttackPower() > 0) parts.add("+${m.effectiveAttackPower()} Атака")
    if (m.effectiveBonusAp() > 0) parts.add("+${m.effectiveBonusAp()} ОД")
    if (m.effectiveFov() > 0) parts.add("+${m.effectiveFov()} Огляд")
    return if (parts.isEmpty()) "Спеціальний тригер активний" else parts.joinToString(" • ")
}
