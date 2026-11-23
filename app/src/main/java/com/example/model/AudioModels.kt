package com.example.model

enum class AmbientTrackProfile(
    val title: String,
    val subtitle: String,
    val biome: TilesetBiome?
) {
    VOID_MENU(
        title = "Тиша Порожнечі",
        subtitle = "Пролог Безодні",
        biome = null
    ),
    FLESH_CATACOMBS(
        title = "Пульс Некро-Плоті",
        subtitle = "Ярус I • Плоть",
        biome = TilesetBiome.FLESH
    ),
    STEEL_FOUNDRY(
        title = "Гул Шлакового Цеху",
        subtitle = "Ярус II • Сталь",
        biome = TilesetBiome.STEEL
    ),
    CRUCIBLE_ABYSS(
        title = "Спів Хтонічного Горнила",
        subtitle = "Ярус III • Горнило",
        biome = TilesetBiome.CRUCIBLE
    );

    companion object {
        fun fromBiome(biome: TilesetBiome): AmbientTrackProfile = when (biome) {
            TilesetBiome.FLESH -> FLESH_CATACOMBS
            TilesetBiome.STEEL -> STEEL_FOUNDRY
            TilesetBiome.CRUCIBLE -> CRUCIBLE_ABYSS
        }
    }
}

data class AudioUiState(
    val activeProfile: AmbientTrackProfile = AmbientTrackProfile.VOID_MENU,
    val combatIntensity: Float = 0f,
    val musicVolume: Float = 0.75f,
    val sfxVolume: Float = 0.85f,
    val isMuted: Boolean = false,
    val isMixerExpanded: Boolean = false
) {
    val isCombatSurgeActive: Boolean
        get() = combatIntensity >= 0.35f
}
