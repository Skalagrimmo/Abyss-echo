package com.example.model

data class FloatingFx(
    val id: Long = System.nanoTime(),
    val gridX: Int,
    val gridY: Int,
    val text: String,
    val colorHex: Long,
    val isCrit: Boolean = false,
    val startTime: Long = System.currentTimeMillis()
)

data class MilestoneBanner(
    val text: String,
    val icon: String = "⚔️",
    val colorHex: Long = 0xFFF59E0B,
    val timestamp: Long = System.currentTimeMillis()
)
