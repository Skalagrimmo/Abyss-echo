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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Item
import com.example.model.ItemCatalog
import com.example.model.Player
import com.example.model.Recipe
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*

@Composable
fun InventorySheet(
    player: Player,
    inventory: List<Item>,
    onUseItem: (Item) -> Unit,
    onCraftItem: (Recipe) -> Unit,
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
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎒 ІНВЕНТАР ТА КРАФТ",
                color = ForgeAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            RetroButton(
                text = "НАЗАД",
                onClick = onBack,
                color = SurfaceVariantIron,
                textColor = BoneIvory,
                testTag = "btn_back_inv"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Resources bar
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderMetal,
            backgroundColor = SurfaceIron
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("БРУХТ: ${player.scrap}", color = ForgeAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("БІОМАСА: ${player.biomass}", color = ToxicGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("ЕСЕНЦІЯ: ${player.essence}", color = SanityCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ПРЕДМЕТИ У РАНЦІ:",
            color = AshGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (inventory.isEmpty()) {
            Text(
                text = "Ранець порожній. Збирайте лут із залишків ворогів.",
                color = AshGrey,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(inventory) { item ->
                    PixelCard(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = "${item.name} (x${item.quantity})",
                                    color = BoneIvory,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.description,
                                    color = AshGrey,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RetroButton(
                                text = "ВЖИТИ",
                                onClick = { onUseItem(item) },
                                color = SanityCyan,
                                textColor = VoidBlack,
                                testTag = "btn_use_${item.id}"
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Crafting recipes section
        Text(
            text = "ПОЛЬОВА КУЗНЯ ТА АЛХІМІЯ ПЛОТІ:",
            color = ForgeAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ItemCatalog.craftRecipes) { recipe ->
                val canCraft = player.scrap >= recipe.scrapCost && player.biomass >= recipe.biomassCost
                PixelCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (canCraft) ForgeAmber else BorderMetal,
                    backgroundColor = SurfaceIron
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recipe.resultItemName,
                                color = BoneIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = recipe.description,
                                color = AshGrey,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (recipe.scrapCost > 0) {
                                    Text("Брухт: ${recipe.scrapCost}", color = ForgeAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                                if (recipe.biomassCost > 0) {
                                    Text("Біомаса: ${recipe.biomassCost}", color = ToxicGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        RetroButton(
                            text = "КРАФТ",
                            onClick = { onCraftItem(recipe) },
                            enabled = canCraft,
                            color = ForgeAmber,
                            textColor = VoidBlack,
                            testTag = "btn_craft_${recipe.id}"
                        )
                    }
                }
            }
        }
    }
}
