package com.example.model

enum class TacticalPropType(
    val title: String,
    val icon: String,
    val maxHp: Int,
    val isExplosive: Boolean,
    val blocksMovement: Boolean,
    val blocksLineOfSight: Boolean,
    val explosionRadius: Int = 1,
    val explosionDamage: Int = 20
) {
    EXPLOSIVE_BARREL(
        "Шлакова Бочка з Пальним",
        "🛢️",
        maxHp = 10,
        isExplosive = true,
        blocksMovement = true,
        blocksLineOfSight = true,
        explosionRadius = 1,
        explosionDamage = 22
    ),
    BARRICADE_CRATE(
        "Іржавий Ящик-Барикада",
        "📦",
        maxHp = 18,
        isExplosive = false,
        blocksMovement = true,
        blocksLineOfSight = true
    ),
    STEAM_PRESSURE_VENT(
        "Парова Настінна Пастка",
        "♨️",
        maxHp = 1,
        isExplosive = false,
        blocksMovement = false,
        blocksLineOfSight = false
    ),
    SPIKE_PLATE(
        "Хтонічні Шипи",
        "⚙️",
        maxHp = 1,
        isExplosive = false,
        blocksMovement = false,
        blocksLineOfSight = false
    )
}

data class TacticalProp(
    val id: String,
    val pos: GridPos,
    val type: TacticalPropType,
    var currentHp: Int = type.maxHp,
    var isTriggered: Boolean = false,
    var isDestroyed: Boolean = false
) {
    val isDead: Boolean get() = isDestroyed || currentHp <= 0
}
