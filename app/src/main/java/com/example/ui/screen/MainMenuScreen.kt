package com.example.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.engine.SoundSynthesizer
import com.example.model.Archetype
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    onStartGame: (Archetype) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedArchetype by remember { mutableStateOf(Archetype.IRON_DESERTER) }
    var isMuted by remember { mutableStateOf(SoundSynthesizer.isMuted) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Art Banner
        Image(
            painter = painterResource(id = R.drawable.img_grim_hero),
            contentDescription = "Арт Ехо Безодні",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(CutCornerShape(6.dp))
                .border(2.dp, ForgeAmber, CutCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "ЕХО БЕЗОДНІ",
            color = ForgeAmber,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "16-БІТНИЙ СИСТЕМНИЙ РОГЛАЙК",
            color = AshGrey,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "«Попіл у горлі, залізо в зубах! Бог Фабрики під нами... Розірви кайдани!»",
            color = EldritchViolet,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sound Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            RetroButton(
                text = if (isMuted) "🔇 ЗВУК: ВИМК" else "🔊 ЗВУК: УВІМК",
                onClick = {
                    isMuted = !isMuted
                    SoundSynthesizer.isMuted = isMuted
                },
                color = SurfaceVariantIron,
                textColor = BoneIvory,
                testTag = "btn_mute_toggle"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "ОБЕРІТЬ АРХЕТИП ПЕРЕД СПУСКОМ:",
            color = AshGrey,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Archetype.values().forEach { arch ->
            val isSelected = selectedArchetype == arch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("archetype_${arch.name}")
                    .clip(CutCornerShape(4.dp))
                    .background(if (isSelected) SurfaceVariantIron else SurfaceIron)
                    .border(2.dp, if (isSelected) ForgeAmber else BorderMetal, CutCornerShape(4.dp))
                    .clickable { selectedArchetype = arch }
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = arch.title,
                            color = if (isSelected) ForgeAmber else BoneIvory,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isSelected) {
                            Text(
                                text = "✔ ОБРАНО",
                                color = ForgeAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = arch.description,
                        color = AshGrey,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ОЗ: ${arch.hp}", color = BloodCrimson, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Глузд: ${arch.sanity}", color = SanityCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Брухт: ${arch.scrap}", color = ForgeAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Біомаса: ${arch.biomass}", color = ToxicGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        RetroButton(
            text = "⚡ РОЗПОЧАТИ ЗАНУРЕННЯ В БЕЗОДНЮ",
            onClick = { onStartGame(selectedArchetype) },
            color = ForgeAmber,
            textColor = VoidBlack,
            testTag = "btn_start_game",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
