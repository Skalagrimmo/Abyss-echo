package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import com.example.model.*
import com.example.ui.components.ComboDisplay
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*

@Composable
fun CombatControls(
    player: Player,
    enemies: List<Enemy>,
    targetEnemyId: String?,
    selectedSkill: CombatSkill,
    onMove: (Int, Int) -> Unit,
    onSelectSkill: (CombatSkill) -> Unit,
    onAttackTarget: (Enemy, CombatSkill) -> Unit,
    onEndTurn: () -> Unit,
    onOpenEvolution: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenCodex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetEnemy = enemies.find { it.id == targetEnemyId } ?: enemies.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceIron)
            .border(2.dp, BorderMetal, CutCornerShape(4.dp))
            .padding(8.dp)
    ) {
        // Target banner + AP & Combo Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Info
            if (targetEnemy != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(VoidBlack, CutCornerShape(3.dp))
                        .border(1.dp, BloodCrimson, CutCornerShape(3.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ЦІЛЬ: ${targetEnemy.archetype.title}",
                        color = BoneIvory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ОЗ: ${targetEnemy.currentHp}/${targetEnemy.maxHp}",
                        color = BloodCrimson,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${targetEnemy.intent.type.desc})",
                        color = ForgeAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "Ворогів поблизу не помічено",
                    color = AshGrey,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // AP Gauge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ОД:",
                    color = BoneIvory,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                for (i in 1..player.maxAp) {
                    val isAvailable = i <= player.currentAp
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                if (isAvailable) SanityCyan else VoidBlack,
                                CutCornerShape(2.dp)
                            )
                            .border(1.dp, if (isAvailable) BoneIvory else BorderMetal, CutCornerShape(2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Combo chain status
        ComboDisplay(comboStage = player.comboStage, comboHits = player.comboHits)

        Spacer(modifier = Modifier.height(8.dp))

        // Skills bar
        Text(
            text = "АРСЕНАЛ ТА МУТАЦІЇ:",
            color = AshGrey,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(player.knownSkills) { skill ->
                val isSelected = skill.id == selectedSkill.id
                val canAfford = player.currentAp >= skill.apCost

                Box(
                    modifier = Modifier
                        .testTag("skill_${skill.id}")
                        .clip(CutCornerShape(4.dp))
                        .background(
                            when {
                                isSelected -> ForgeAmber.copy(alpha = 0.25f)
                                canAfford -> VoidBlack
                                else -> AshGrey.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) ForgeAmber else if (canAfford) BorderMetal else Color.DarkGray,
                            CutCornerShape(4.dp)
                        )
                        .clickable(enabled = canAfford) {
                            onSelectSkill(skill)
                            if (targetEnemy != null) {
                                onAttackTarget(targetEnemy, skill)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = skill.name,
                                color = if (canAfford) BoneIvory else AshGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "[${skill.apCost} ОД]",
                                color = SanityCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Урон: ${skill.baseDamage} | Дальн: ${skill.range}",
                            color = AshGrey,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom action deck: D-Pad on left, quick menu and End Turn on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactical D-Pad for precise grid movement
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DPadButton("▲", testTag = "dpad_up") { onMove(0, -1) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DPadButton("◀", testTag = "dpad_left") { onMove(-1, 0) }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(VoidBlack, CutCornerShape(4.dp))
                            .border(1.dp, BorderMetal, CutCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✚", color = AshGrey, fontSize = 12.sp)
                    }
                    DPadButton("▶", testTag = "dpad_right") { onMove(1, 0) }
                }
                DPadButton("▼", testTag = "dpad_down") { onMove(0, 1) }
            }

            // Central / Right Menu Shortcuts & End Turn
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RetroButton(
                        text = "🧬 ЕВОЛЮЦІЯ",
                        onClick = onOpenEvolution,
                        color = EldritchViolet,
                        textColor = BoneIvory,
                        testTag = "btn_evolution",
                        modifier = Modifier.weight(1f)
                    )
                    RetroButton(
                        text = "🎒 ІНВЕНТАР",
                        onClick = onOpenInventory,
                        color = SurfaceVariantIron,
                        textColor = BoneIvory,
                        testTag = "btn_inventory",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RetroButton(
                        text = "📜 КОДЕКС",
                        onClick = onOpenCodex,
                        color = SurfaceVariantIron,
                        textColor = AshGrey,
                        testTag = "btn_codex",
                        modifier = Modifier.weight(0.9f)
                    )
                    RetroButton(
                        text = "⏳ ЗАВЕРШИТИ ХІД",
                        onClick = onEndTurn,
                        color = SlagEmber,
                        textColor = BoneIvory,
                        testTag = "btn_end_turn",
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DPadButton(
    symbol: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .testTag(testTag)
            .clip(CutCornerShape(4.dp))
            .background(SurfaceVariantIron)
            .border(1.5.dp, BorderMetal, CutCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = BoneIvory,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
