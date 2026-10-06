package com.example.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroButton
import com.example.ui.theme.*
import com.example.viewmodel.GameUiState

@Composable
fun GameOverSheet(
    state: GameUiState,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWin = state.isVictory

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Banner Image
        Image(
            painter = painterResource(id = R.drawable.img_grim_hero),
            contentDescription = "Фон Безодні",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(CutCornerShape(6.dp))
                .border(2.dp, if (isWin) ForgeAmber else BloodCrimson, CutCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isWin) "🏆 ТРІУМФ НАД БОГОМ ФАБРИКИ" else "💀 БЕЗОДНЯ ПОГЛИНУЛА ТЕБЕ",
            color = if (isWin) ForgeAmber else BloodCrimson,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isWin) {
                "Ви розірвали парові кайдани тисячолітнього колоса. Над глибинами розвіявся туман."
            } else {
                "Ваша плоть та залізо злилися з нескінченним шлаком забутих штолень."
            },
            color = AshGrey,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats summary
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderMetal,
            backgroundColor = SurfaceIron
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Пройдено рівнів: ${state.currentFloorNumber} з 3", color = BoneIvory, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Вижито ходів: ${state.turnCount}", color = BoneIvory, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Прищеплено мутацій: ${state.player.activeMutations.size}", color = EldritchViolet, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Кінцеве спотворення: ${state.player.corruption}%", color = ToxicGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Ухвалено моральних рішень: ${state.storyChronicle.size}", color = ForgeAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        RetroButton(
            text = "НОВЕ ЗАНУРЕННЯ В БЕЗОДНЮ",
            onClick = onRestart,
            color = if (isWin) ForgeAmber else BloodCrimson,
            textColor = if (isWin) VoidBlack else BoneIvory,
            testTag = "btn_restart"
        )
    }
}
