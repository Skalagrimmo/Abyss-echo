package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.model.DilemmaChoice
import com.example.model.MoralDilemma
import com.example.ui.components.PixelCard
import com.example.ui.theme.*

@Composable
fun DilemmaDialog(
    dilemma: MoralDilemma,
    onChooseOption: (DilemmaChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.92f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        PixelCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BloodCrimson, CutCornerShape(6.dp)),
            borderColor = BloodCrimson,
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚖️ ДИЛЕМА ВИЖИВАННЯ",
                        color = BloodCrimson,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dilemma.title,
                    color = BoneIvory,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = dilemma.subtitle,
                    color = ForgeAmber,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Narrative block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(4.dp))
                        .background(VoidBlack)
                        .border(1.dp, BorderMetal, CutCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = dilemma.narrative,
                        color = BoneIvory.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ОБЕРІТЬ ВАШ ВЧИНОК (НАСЛІДКИ НЕЗВОРОТНІ):",
                    color = AshGrey,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Choices list
                dilemma.choices.forEachIndexed { idx, choice ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("dilemma_choice_$idx")
                            .clip(CutCornerShape(4.dp))
                            .background(SurfaceVariantIron)
                            .border(1.5.dp, BorderMetal, CutCornerShape(4.dp))
                            .clickable { onChooseOption(choice) }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "${idx + 1}. ${choice.title}",
                                color = ForgeAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = choice.description,
                                color = AshGrey,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Impact preview tags
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (choice.hpChange != 0) {
                                    Text(
                                        text = "${if (choice.hpChange > 0) "+" else ""}${choice.hpChange} ОЗ",
                                        color = if (choice.hpChange > 0) ToxicGreen else BloodCrimson,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (choice.sanityChange != 0) {
                                    Text(
                                        text = "${if (choice.sanityChange > 0) "+" else ""}${choice.sanityChange} Глузд",
                                        color = if (choice.sanityChange > 0) SanityCyan else EldritchViolet,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (choice.corruptionChange != 0) {
                                    Text(
                                        text = "${if (choice.corruptionChange > 0) "+" else ""}${choice.corruptionChange}% Спотворення",
                                        color = EldritchViolet,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (choice.factionAffected != null) {
                                    Text(
                                        text = "[${choice.factionAffected.title}: ${if (choice.factionReputationChange > 0) "+" else ""}${choice.factionReputationChange}]",
                                        color = ForgeAmber,
                                        fontSize = 9.sp,
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
