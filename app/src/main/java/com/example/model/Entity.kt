package com.example.model

enum class EnemyArchetype(
    val title: String,
    val maxHp: Int,
    val armor: Int,
    val baseDamage: Int,
    val range: Int,
    val biomassReward: Int,
    val scrapReward: Int,
    val glyphChar: Char,
    val colorHex: Long
) {
    CRAWLING_SPIDER(
        "Повзуча Спороноска",
        18, 0, 6, 1, 6, 2, '§', 0xFF4ADE80
    ),
    CHTHONIC_CULTIST(
        "Хтонічний Жрець",
        26, 1, 9, 2, 8, 4, 'Ж', 0xFFA855F7
    ),
    IRON_AUTOMATON(
        "Фабричний Коваль-Автомат",
        42, 4, 14, 1, 3, 16, 'Ω', 0xFFF59E0B
    ),
    SLAG_HOUND(
        "Шлаковий Птах-Гончак",
        22, 1, 11, 1, 8, 5, 'ψ', 0xFFEF4444
    ),
    CRUCIBLE_TITAN(
        "Титан Горнила (Бос)",
        110, 6, 24, 2, 35, 45, 'Ψ', 0xFFDC2626
    )
}

enum class EnemyIntentType(val desc: String) {
    ATTACK_MELEE("Готує нищівний удар"),
    ATTACK_RANGED("Цілиться на відстані"),
    CAST_CURSE("Співає хтонічний мантр"),
    APPROACH("Наближається до здобичі"),
    DEFEND("Укріплює броню")
}

data class EnemyIntent(
    val type: EnemyIntentType,
    val expectedDamage: Int,
    val targetPos: GridPos?
)

data class Enemy(
    val id: String,
    val archetype: EnemyArchetype,
    var pos: GridPos,
    var currentHp: Int,
    var maxHp: Int = archetype.maxHp,
    var armor: Int = archetype.armor,
    val statuses: MutableList<StatusEffect> = mutableListOf(),
    var intent: EnemyIntent = EnemyIntent(EnemyIntentType.APPROACH, archetype.baseDamage, null),
    var isAlerted: Boolean = false
) {
    val isDead: Boolean get() = currentHp <= 0

    fun hasStatus(type: StatusEffectType): Boolean = statuses.any { it.type == type }
}

data class Player(
    var pos: GridPos,
    var currentHp: Int,
    var maxHp: Int,
    var armor: Int,
    var sanity: Int,
    var maxSanity: Int = 100,
    var corruption: Int = 0,
    var maxCorruption: Int = 100,
    var currentAp: Int = 3,
    var maxAp: Int = 3,
    var fovRadius: Int = 6,
    var torchTurns: Int = 60,
    // Resources
    var scrap: Int = 15,
    var biomass: Int = 10,
    var essence: Int = 0,
    // Equipment & Mutations
    val activeMutations: MutableList<Mutation> = mutableListOf(),
    val knownSkills: MutableList<CombatSkill> = mutableListOf(),
    val statuses: MutableList<StatusEffect> = mutableListOf(),
    var comboStage: ComboStage = ComboStage.READY,
    var comboHits: Int = 0,
    var archetype: Archetype = Archetype.IRON_DESERTER
) {
    val isDead: Boolean get() = currentHp <= 0

    fun hasStatus(type: StatusEffectType): Boolean = statuses.any { it.type == type }

    fun totalAttackPower(): Int {
        var bonus = activeMutations.sumOf { it.bonusAttackPower }
        if (hasStatus(StatusEffectType.ADRENALINE)) bonus += 5
        if (sanity < 30) bonus += 4 // Frenzy of despair
        return 10 + bonus
    }

    fun totalArmor(): Int {
        var base = armor + activeMutations.sumOf { it.bonusArmor }
        if (hasStatus(StatusEffectType.IRON_SKIN)) base += 4
        return base
    }
}
