package com.example.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Faction
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*
import com.example.viewmodel.StoryRecord

@Composable
fun StoryCodexSheet(
    chronicle: List<StoryRecord>,
    factions: Map<Faction, Int>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📜 ХРОНІКА ТА ФРАКЦІЇ",
                color = BoneIvory,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            RetroButton(
                text = "НАЗАД",
                onClick = onBack,
                color = SurfaceVariantIron,
                textColor = BoneIvory,
                testTag = "btn_back_codex"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Factions Standing
        Text(
            text = "РЕПУТАЦІЯ СЕРЕД СИЛ БЕЗОДНІ:",
            color = AshGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        factions.forEach { (faction, rep) ->
            PixelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                borderColor = BorderMetal,
                backgroundColor = SurfaceIron
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = faction.title,
                            color = ForgeAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = faction.description,
                            color = AshGrey,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "${if (rep > 0) "+" else ""}$rep",
                        color = if (rep >= 0) ToxicGreen else BloodCrimson,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chronicle of Deeds
        Text(
            text = "ЛІТОПИС ВАШИХ ВЧИНКІВ:",
            color = AshGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (chronicle.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceIron, CutCornerShape(4.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ви ще не стикалися зі складними виборами. Продовжуйте спуск у безодню.",
                    color = AshGrey,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(chronicle) { record ->
                    PixelCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderMetal,
                        backgroundColor = SurfaceIron
                    ) {
                        Text(
                            text = "Рівень ${record.floor}: ${record.title}",
                            color = ForgeAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Вибір: ${record.choiceMade}",
                            color = BoneIvory,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = record.consequence,
                            color = AshGrey,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
