package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.Archetype
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroStatBar
import com.example.ui.components.TimePhaseBanner
import com.example.ui.screen.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState

@Composable
fun MainGameScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoidBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.currentScreen) {
                ScreenState.MAIN_MENU -> {
                    MainMenuScreen(
                        onStartGame = { arch -> viewModel.startNewGame(arch) }
                    )
                }
                ScreenState.GAME_OVER -> {
                    GameOverSheet(
                        state = state,
                        onRestart = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
                    )
                }
                ScreenState.EVOLUTION_SCREEN -> {
                    EvolutionSheet(
                        player = state.player,
                        onAcquireMutation = { mut -> viewModel.acquireMutation(mut) },
                        onBack = { viewModel.navigateTo(ScreenState.DUNGEON) }
                    )
                }
                ScreenState.INVENTORY_SCREEN -> {
                    InventorySheet(
                        player = state.player,
                        inventory = state.availableInventory,
                        onUseItem = { item -> viewModel.useItem(item) },
                        onCraftItem = { recipe -> viewModel.craftItem(recipe) },
                        onBack = { viewModel.navigateTo(ScreenState.DUNGEON) }
                    )
                }
                ScreenState.CODEX_SCREEN -> {
                    StoryCodexSheet(
                        chronicle = state.storyChronicle,
                        factions = state.factionReputations,
                        onBack = { viewModel.navigateTo(ScreenState.DUNGEON) }
                    )
                }
                ScreenState.DUNGEON, ScreenState.DILEMMA_POPUP -> {
                    DungeonPlayLayout(
                        state = state,
                        viewModel = viewModel
                    )

                    // Overlay dilemma if active
                    if (state.activeDilemma != null) {
                        DilemmaDialog(
                            dilemma = state.activeDilemma!!,
                            onChooseOption = { choice -> viewModel.chooseDilemmaOption(choice) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DungeonPlayLayout(
    state: com.example.viewmodel.GameUiState,
    viewModel: GameViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Time Phase & Threat Meter
        TimePhaseBanner(
            phase = state.timePhase,
            turnCount = state.turnCount
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vitals HUD
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = BorderMetal,
            backgroundColor = SurfaceIron
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Column: HP & Sanity
                Column(modifier = Modifier.weight(1f)) {
                    RetroStatBar(
                        label = "ОЗ",
                        current = state.player.currentHp,
                        max = state.player.maxHp,
                        barColor = BloodCrimson,
                        icon = "❤️"
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    RetroStatBar(
                        label = "Глузд",
                        current = state.player.sanity,
                        max = state.player.maxSanity,
                        barColor = SanityCyan,
                        icon = "🧠"
                    )
                }

                // Right Column: Corruption & Torch
                Column(modifier = Modifier.weight(1f)) {
                    RetroStatBar(
                        label = "Спотворення",
                        current = state.player.corruption,
                        max = state.player.maxCorruption,
                        barColor = EldritchViolet,
                        icon = "🧬"
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    RetroStatBar(
                        label = "Смолоскип",
                        current = state.player.torchTurns,
                        max = 60,
                        barColor = ForgeAmber,
                        icon = "🔥"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Center Area: Dungeon Canvas (weight 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            DungeonCanvas(
                tiles = viewModel.currentDungeon?.tiles,
                player = state.player,
                enemies = state.enemies,
                targetEnemyId = state.targetEnemyId,
                onTileTap = { pos ->
                    val dx = (pos.x - state.player.pos.x).coerceIn(-1, 1)
                    val dy = (pos.y - state.player.pos.y).coerceIn(-1, 1)
                    if (dx != 0 || dy != 0) {
                        viewModel.movePlayer(dx, dy)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Mini Combat Log overlay at the bottom of the canvas
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(VoidBlack.copy(alpha = 0.85f), CutCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .border(1.dp, BorderMetal.copy(alpha = 0.5f), CutCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                val logListState = rememberLazyListState()
                LazyColumn(state = logListState, modifier = Modifier.fillMaxSize()) {
                    items(state.logs.take(3)) { log ->
                        Text(
                            text = log,
                            color = BoneIvory,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Tactical Bar
        CombatControls(
            player = state.player,
            enemies = state.enemies,
            targetEnemyId = state.targetEnemyId,
            selectedSkill = state.selectedSkill,
            onMove = { dx, dy -> viewModel.movePlayer(dx, dy) },
            onSelectSkill = { skill -> viewModel.selectSkill(skill) },
            onAttackTarget = { enemy, skill -> viewModel.attackEnemy(enemy, skill) },
            onEndTurn = { viewModel.endTurn() },
            onOpenEvolution = { viewModel.navigateTo(ScreenState.EVOLUTION_SCREEN) },
            onOpenInventory = { viewModel.navigateTo(ScreenState.INVENTORY_SCREEN) },
            onOpenCodex = { viewModel.navigateTo(ScreenState.CODEX_SCREEN) }
        )
    }
}
