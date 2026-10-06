package com.example.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
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
import com.example.model.*
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.components.RetroStatBar
import com.example.ui.theme.*

@Composable
fun EvolutionSheet(
    player: Player,
    onAcquireMutation: (Mutation) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedCategory by remember { mutableStateOf<MutationCategory?>(null) }

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
                fontSize = 18.sp,
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

        Spacer(modifier = Modifier.height(10.dp))

        // Resource summary
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderMetal,
            backgroundColor = SurfaceIron
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("БІОМАСА", color = ToxicGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${player.biomass}", color = BoneIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("БРУХТ", color = ForgeAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${player.scrap}", color = BoneIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ГЛУЗД", color = SanityCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${player.sanity}", color = BoneIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("СПОТВОРЕННЯ", color = EldritchViolet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("${player.corruption}%", color = BoneIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Mutation Slots
        Text(
            text = "АКТИВНІ МУТАЦІЇ ТА ІМПЛАНТИ (${player.activeMutations.size}/5):",
            color = AshGrey,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MutationSlot.values().forEach { slot ->
                val activeInSlot = player.activeMutations.find { it.slot == slot }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CutCornerShape(4.dp))
                        .background(if (activeInSlot != null) SurfaceVariantIron else VoidBlack)
                        .border(1.dp, if (activeInSlot != null) EldritchViolet else BorderMetal, CutCornerShape(4.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = slot.name.take(4),
                            color = AshGrey,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = activeInSlot?.name?.take(6) ?: "Пусто",
                            color = if (activeInSlot != null) BoneIvory else Color.DarkGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val allCats = listOf<MutationCategory?>(null) + MutationCategory.values().toList()
            allCats.forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
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

        Spacer(modifier = Modifier.height(10.dp))

        // Available mutations list
        val displayedMutations = MutationCatalog.allMutations.filter {
            selectedCategory == null || it.category == selectedCategory
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(displayedMutations) { mutation ->
                val isOwned = player.activeMutations.any { it.id == mutation.id }
                val canAfford = player.biomass >= mutation.biomassCost &&
                        player.scrap >= mutation.scrapCost &&
                        player.sanity >= mutation.sanityCost

                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = when {
                        isOwned -> EldritchViolet
                        canAfford -> ForgeAmber
                        else -> BorderMetal
                    },
                    backgroundColor = if (isOwned) SurfaceVariantIron else SurfaceIron
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
                                    text = "[${mutation.slot.title}]",
                                    color = AshGrey,
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
                            // Cost line
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (mutation.biomassCost > 0) {
                                    Text("Біомаса: ${mutation.biomassCost}", color = ToxicGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (mutation.scrapCost > 0) {
                                    Text("Брухт: ${mutation.scrapCost}", color = ForgeAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (mutation.sanityCost > 0) {
                                    Text("Втрата глузду: ${mutation.sanityCost}", color = SanityCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text("Спотворення: +${mutation.corruptionIncrease}%", color = EldritchViolet, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isOwned) {
                            Text(
                                text = "ВЖЕ ПРИЩЕПЛЕНО",
                                color = EldritchViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            RetroButton(
                                text = "ПРИЩЕПИТИ",
                                onClick = { onAcquireMutation(mutation) },
                                enabled = canAfford,
                                color = ForgeAmber,
                                textColor = VoidBlack,
                                testTag = "btn_mutate_${mutation.id}"
                            )
                        }
                    }
                }
            }
        }
    }
}
