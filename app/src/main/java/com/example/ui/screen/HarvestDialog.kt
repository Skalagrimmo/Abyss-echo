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
import com.example.model.DefeatedHarvest
import com.example.model.HarvestMutationOption
import com.example.ui.components.PixelCard
import com.example.ui.theme.*

@Composable
fun HarvestDialog(
    harvest: DefeatedHarvest,
    onSelectOption: (HarvestMutationOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.94f))
            .padding(16.dp),
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
                        text = "ПЕРЕМОГА!",
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
                    text = "Трофей з поваленого: ${harvest.enemyTitle}",
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ОБЕРІТЬ НАПРЯМОК ЕВОЛЮЦІЇ ВАШОГО ТІЛА:",
                    color = ForgeAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Options list
                harvest.options.forEachIndexed { idx, opt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("harvest_opt_$idx")
                            .clip(CutCornerShape(4.dp))
                            .background(SurfaceVariantIron)
                            .border(1.5.dp, BorderMetal, CutCornerShape(4.dp))
                            .clickable { onSelectOption(opt) }
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
                                    color = if (opt.bonusAttackPower > 0 || opt.grantedSkill != null) ForgeAmber else ToxicGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "[${opt.slot.title.take(6)}]",
                                    color = AshGrey,
                                    fontSize = 9.sp,
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
                                    Text("+${opt.bonusMaxHp} Макс ОЗ", color = BloodCrimson, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                                if (opt.grantedSkill != null) {
                                    Text("✨ Здібність: [${opt.grantedSkill.name}]", color = ForgeAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
