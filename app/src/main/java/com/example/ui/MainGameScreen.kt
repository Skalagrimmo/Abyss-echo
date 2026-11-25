package com.example.ui

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.AmbientAudioEngine
import com.example.model.Archetype
import com.example.ui.components.AudioMixerOverlay
import com.example.ui.components.PixelCard
import com.example.ui.components.RetroStatBar
import com.example.ui.components.TimePhaseBanner
import com.example.ui.components.TrackIndicatorPill
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioPrefs = remember(context) {
        context.getSharedPreferences("echo_abyss_audio_prefs", Context.MODE_PRIVATE)
    }
    var prefsHydrated by remember { mutableStateOf(false) }

    LaunchedEffect(audioPrefs) {
        val musicVol = audioPrefs.getFloat("music_volume", 0.75f)
        val sfxVol = audioPrefs.getFloat("sfx_volume", 0.85f)
        val muted = audioPrefs.getBoolean("is_muted", false)
        viewModel.applyPersistedAudioSettings(musicVol, sfxVol, muted)
        AmbientAudioEngine.setProfile(state.audioState.activeProfile)
        prefsHydrated = true
    }

    LaunchedEffect(
        prefsHydrated,
        state.audioState.musicVolume,
        state.audioState.sfxVolume,
        state.audioState.isMuted
    ) {
        if (!prefsHydrated) return@LaunchedEffect
        audioPrefs.edit()
            .putFloat("music_volume", state.audioState.musicVolume)
            .putFloat("sfx_volume", state.audioState.sfxVolume)
            .putBoolean("is_muted", state.audioState.isMuted)
            .apply()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> AmbientAudioEngine.pause()
                Lifecycle.Event.ON_RESUME -> AmbientAudioEngine.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                        audioState = state.audioState,
                        onToggleMute = { viewModel.toggleMute() },
                        onMusicVolumeChange = { viewModel.setMusicVolume(it) },
                        onSfxVolumeChange = { viewModel.setSfxVolume(it) },
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
                        activeSlotPulse = state.activeSlotPulse,
                        onAcquireMutation = { mut -> viewModel.acquireMutation(mut) },
                        onIntegrateBioBank = { organId -> viewModel.integrateBioBankOrgan(organId) },
                        onSalvageBioBank = { organId -> viewModel.salvageBioBankOrgan(organId) },
                        onRemoveSlot = { slot -> viewModel.removeSlotMutation(slot) },
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

                    // Overlay post-victory biomechanical harvest mutation if active
                    if (state.pendingHarvest != null) {
                        HarvestDialog(
                            harvest = state.pendingHarvest!!,
                            player = state.player,
                            onSelectOption = { opt -> viewModel.applyHarvestMutation(opt) },
                            onStoreInBioBank = { opt -> viewModel.storeHarvestOrganInBioBank(opt) }
                        )
                    }

                    // Expandable HUD audio mixer overlay
                    AudioMixerOverlay(
                        visible = state.audioState.isMixerExpanded,
                        audioState = state.audioState,
                        onToggleMute = { viewModel.toggleMute() },
                        onMusicVolumeChange = { viewModel.setMusicVolume(it) },
                        onSfxVolumeChange = { viewModel.setSfxVolume(it) },
                        onDismiss = { viewModel.dismissAudioMixer() }
                    )
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

        // Dynamic Biome Track & Combat Surge HUD Indicator
        TrackIndicatorPill(
            audioState = state.audioState,
            onClick = { viewModel.toggleAudioMixer() }
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

        // Center Area: Dungeon Canvas (takes full responsive space for gameplay immersion)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            DungeonCanvas(
                tiles = viewModel.currentDungeon?.tiles,
                player = state.player,
                enemies = state.enemies,
                props = state.activeProps,
                validTargetCells = state.validTargetCells,
                tacticalGrid = state.tacticalGrid,
                routePreview = state.routePreview,
                selectedSkill = state.selectedSkill,
                targetEnemyId = state.targetEnemyId,
                floatingFxList = state.floatingFxList,
                activeAttackFx = state.activeAttackFx,
                activeExplosions = state.activeExplosions,
                activeMutationSurges = state.activeMutationSurges,
                activeEnemyAnims = state.activeEnemyAnims,
                activeBanner = state.activeBanner,
                onTileTap = { pos ->
                    viewModel.handleTileClick(pos)
                },
                onClearRoutePreview = {
                    viewModel.clearRoutePreview()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Tactical Bar
        CombatControls(
            player = state.player,
            enemies = state.enemies,
            targetEnemyId = state.targetEnemyId,
            selectedSkill = state.selectedSkill,
            tacticalGrid = state.tacticalGrid,
            targetForecast = state.targetForecast,
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
