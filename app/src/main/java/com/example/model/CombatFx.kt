package com.example.model

import kotlin.random.Random

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

data class AttackVisualFx(
    val id: Long = System.nanoTime(),
    val from: GridPos,
    val to: GridPos,
    val distanceType: AttackDistanceType,
    val colorHex: Long = 0xFFEF4444,
    val durationMs: Long = 320L,
    val startTime: Long = System.currentTimeMillis()
)

data class ExplosionDebris(
    val vx: Float,
    val vy: Float,
    val colorHex: Long,
    val pixelSize: Float
)

data class ExplosionVisualFx(
    val id: Long = System.nanoTime(),
    val epicenter: GridPos,
    val radiusTiles: Float = 1.6f,
    val debris: List<ExplosionDebris> = createDefaultDebris(),
    val durationMs: Long = 480L,
    val startTime: Long = System.currentTimeMillis()
) {
    companion object {
        fun createDefaultDebris(): List<ExplosionDebris> {
            val rng = Random.Default
            val colors = listOf(0xFFF59E0B, 0xFFEF4444, 0xFF78350F, 0xFFD97706, 0xFFFFFFFF)
            return (0 until 14).map {
                val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
                val speed = 25f + rng.nextFloat() * 65f
                ExplosionDebris(
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    colorHex = colors[rng.nextInt(colors.size)],
                    pixelSize = 3f + rng.nextFloat() * 4f
                )
            }
        }
    }
}

data class MutationSurgeFx(
    val id: Long = System.nanoTime(),
    val playerPos: GridPos,
    val mutationName: String,
    val runeGlyph: String = "🧬",
    val colorHex: Long = 0xFF06B6D4,
    val durationMs: Long = 750L,
    val startTime: Long = System.currentTimeMillis()
)

data class SlotIntegrationPulseFx(
    val id: Long = System.nanoTime(),
    val slot: MutationSlot,
    val category: MutationCategory,
    val isUpgrade: Boolean = false,
    val tier: Int = 1,
    val durationMs: Long = 720L,
    val startTime: Long = System.currentTimeMillis()
)

enum class EnemyAnimPhase {
    IDLE,
    ATTACK,
    HURT
}

data class EnemyAnimFx(
    val id: Long = System.nanoTime(),
    val enemyId: String,
    val phase: EnemyAnimPhase,
    val dirX: Int = 0,
    val dirY: Int = 0,
    val durationMs: Long = 340L,
    val startTime: Long = System.currentTimeMillis()
)
