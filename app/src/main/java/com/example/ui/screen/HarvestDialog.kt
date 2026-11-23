package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.*

@Composable
fun HarvestDialog(
    harvest: DefeatedHarvest,
    onSelectOption: (HarvestMutationOption) -> Unit,
    onStoreInBioBank: (HarvestMutationOption) -> Unit = {},
    player: Player? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.94f))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, EldritchViolet, CutCornerShape(6.dp)),
            borderColor = EldritchViolet,
            backgroundColor = SurfaceIron
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧬 ЖНИВА ПЛОТІ ТА СТАЛІ",
                        color = EldritchViolet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "ТРОФЕЙ ЗДОБУТО",
                        color = ForgeAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Здобуто: ${harvest.organName}",
                    color = BoneIvory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Носій: ${harvest.enemyTitle}",
                    color = AshGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Organ description box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(4.dp))
                        .background(VoidBlack)
                        .border(1.dp, BorderMetal, CutCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = harvest.organDescription,
                        color = BoneIvory.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ІНТЕГРУВАТИ В БІЛД АБО ЗБЕРЕГТИ В БІО-БАНК:",
                    color = ForgeAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                harvest.options.forEachIndexed { idx, opt ->
                    val branchColor = when (opt.category) {
                        MutationCategory.CHTHONIC_FLESH -> ToxicGreen
                        MutationCategory.IRON_CYBERNETICS -> ForgeAmber
                        MutationCategory.OCCULT_SIGIL -> EldritchViolet
                    }
                    val preview = if (!opt.isResourceSalvage && player != null) {
                        val candidate = EvolutionEngine.convertHarvestOptionToMutation(harvest, opt)
                        EvolutionEngine.previewIntegration(player, candidate)
                    } else {
                        null
                    }

                    val previewLabel = when (preview) {
                        is SlotIntegrationPreview.NewSlot -> "НОВИЙ СЛОТ [I]"
                        is SlotIntegrationPreview.UpgradeRank -> "ЕВОЛЮЦІЯ [I→${preview.nextTier}]"
                        is SlotIntegrationPreview.ReplaceOrgan -> "ЗАМІНИТИ: ${preview.existing.name.take(12)}"
                        is SlotIntegrationPreview.MaxRankConflict -> "СЛОТ III РАНГУ (УТИЛІЗ.)"
                        null -> null
                    }
                    val previewColor = when (preview) {
                        is SlotIntegrationPreview.UpgradeRank -> SanityCyan
                        is SlotIntegrationPreview.ReplaceOrgan -> BloodCrimson
                        is SlotIntegrationPreview.MaxRankConflict -> AshGrey
                        else -> ToxicGreen
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(CutCornerShape(4.dp))
                            .background(SurfaceVariantIron)
                            .border(
                                1.5.dp,
                                if (opt.isResourceSalvage) BorderMetal else branchColor.copy(alpha = 0.65f),
                                CutCornerShape(4.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${opt.title}",
                                    color = if (opt.isResourceSalvage) ToxicGreen else BoneIvory,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!opt.isResourceSalvage) {
                                    Text(
                                        text = "[${opt.slot.title.substringBefore(" /")}]",
                                        color = branchColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            if (previewLabel != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "• ${opt.category.title} • $previewLabel",
                                    color = previewColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = opt.subtitle,
                                color = AshGrey,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            // Stat change tags
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (opt.bonusMaxHp > 0) {
                                    Text("+${opt.bonusMaxHp} ОЗ", color = BloodCrimson, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (opt.bonusArmor > 0) {
                                    Text("+${opt.bonusArmor} Броня", color = SanityCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (opt.bonusAttackPower > 0) {
                                    Text("+${opt.bonusAttackPower} Атака", color = ForgeAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (opt.bonusAp > 0) {
                                    Text("+${opt.bonusAp} ОД", color = SanityCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (opt.bonusFov > 0) {
                                    Text("+${opt.bonusFov} Огляд", color = BoneIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                if (opt.corruptionDelta != 0) {
                                    Text("${if (opt.corruptionDelta > 0) "+" else ""}${opt.corruptionDelta}% Спотв.", color = EldritchViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (opt.passiveTrait != null || opt.grantedSkill != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = buildString {
                                        if (opt.passiveTrait != null) append("✨ ${opt.passiveTrait}")
                                        if (opt.grantedSkill != null) {
                                            if (opt.passiveTrait != null) append("  |  ")
                                            append("⚡ [${opt.grantedSkill.name}]")
                                        }
                                    },
                                    color = ForgeAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (opt.isResourceSalvage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("harvest_opt_$idx")
                                        .clip(CutCornerShape(4.dp))
                                        .background(ToxicGreen.copy(alpha = 0.2f))
                                        .border(1.dp, ToxicGreen, CutCornerShape(4.dp))
                                        .clickable { onSelectOption(opt) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚗️ ПЕРЕРОБИТИ В РЕСУРСИ",
                                        color = ToxicGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                val graftBtnText = when (preview) {
                                    is SlotIntegrationPreview.UpgradeRank -> "🧬 ЕВОЛЮЦІЯ [${preview.nextTier}]"
                                    is SlotIntegrationPreview.ReplaceOrgan -> "🔄 ЗАМІНИТИ"
                                    is SlotIntegrationPreview.MaxRankConflict -> "♻️ УТИЛІЗУВАТИ"
                                    else -> "🧬 ПРИЩЕПИТИ"
                                }
                                val graftBtnColor = when (preview) {
                                    is SlotIntegrationPreview.UpgradeRank -> SanityCyan
                                    is SlotIntegrationPreview.ReplaceOrgan -> BloodCrimson
                                    else -> ForgeAmber
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("harvest_opt_$idx")
                                            .clip(CutCornerShape(4.dp))
                                            .background(graftBtnColor)
                                            .border(1.dp, BoneIvory.copy(alpha = 0.5f), CutCornerShape(4.dp))
                                            .clickable { onSelectOption(opt) }
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = graftBtnText,
                                            color = VoidBlack,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("harvest_biobank_$idx")
                                            .clip(CutCornerShape(4.dp))
                                            .background(AbyssalDark)
                                            .border(1.dp, SanityCyan, CutCornerShape(4.dp))
                                            .clickable { onStoreInBioBank(opt) }
                                            .padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🧫 В БІО-БАНК",
                                            color = SanityCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
