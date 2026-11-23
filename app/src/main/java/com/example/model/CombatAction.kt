package com.example.model

enum class AttackDistanceType(
    val title: String,
    val icon: String,
    val description: String
) {
    MELEE("Ближній бій", "⚔️", "Впритул (1 клітинка). Потужний фізичний або кібернетичний удар."),
    REACH("Хлист / Спис", "🗡️", "Подовжена дальність (2 клітинки). Дозволяє бити через перешкоди."),
    RANGED("Дистанційний", "🎯", "Прямий приціл (3–4 клітинки). Потребує прямої лінії огляду."),
    AREA("Удар по площі", "💥", "Зональний вибух (радіус навколо цілі/себе). Вражає бочки та групи.")
}

enum class ComboStage {
    READY,
    LIGHT_1,
    LIGHT_2,
    HEAVY_PRIMED
}

data class CombatSkill(
    val id: String,
    val name: String,
    val description: String,
    val apCost: Int,
    val range: Int,
    val baseDamage: Int,
    val damageType: DamageType,
    val distanceType: AttackDistanceType = AttackDistanceType.MELEE,
    val appliesStatus: StatusEffectType? = null,
    val statusTurns: Int = 0,
    val isAreaEffect: Boolean = false,
    val areaRadius: Int = 1,
    val pushesTarget: Boolean = false,
    val healsSelf: Int = 0,
    val selfHpCost: Int = 0
)

object SkillCatalog {
    val strikeLight = CombatSkill(
        id = "skill_light_strike",
        name = "Швидкий Випад",
        description = "Точний легкий удар. Будує комбо-ланцюг (Світло 1 -> Світло 2).",
        apCost = 1,
        range = 1,
        baseDamage = 8,
        damageType = DamageType.PHYSICAL,
        distanceType = AttackDistanceType.MELEE
    )

    val strikeHeavy = CombatSkill(
        id = "skill_heavy_cleave",
        name = "Важкий Розкол",
        description = "Потужний розмах, що пробиває броню і відкидає ворога у стіну чи бочку.",
        apCost = 2,
        range = 1,
        baseDamage = 18,
        damageType = DamageType.PHYSICAL,
        distanceType = AttackDistanceType.MELEE,
        pushesTarget = true
    )

    val throwTorch = CombatSkill(
        id = "skill_throw_torch",
        name = "Кидок Факела",
        description = "Кидає палаючий шматок вугілля. Підпалює клітинку або ворога на відстані.",
        apCost = 1,
        range = 3,
        baseDamage = 6,
        damageType = DamageType.FIRE,
        distanceType = AttackDistanceType.RANGED,
        appliesStatus = StatusEffectType.BURNING,
        statusTurns = 3
    )

    val siphonLash = CombatSkill(
        id = "skill_siphon_lash",
        name = "Хтонічний Хлист",
        description = "Вистрілює біо-щупальцем на 2 клітинки (навіть через барикади). Завдає урон і лікує.",
        apCost = 2,
        range = 2,
        baseDamage = 12,
        damageType = DamageType.ELDRITCH,
        distanceType = AttackDistanceType.REACH,
        healsSelf = 7,
        appliesStatus = StatusEffectType.BLEEDING,
        statusTurns = 2
    )

    val pistonSlam = CombatSkill(
        id = "skill_piston_slam",
        name = "Гідравлічний Удар",
        description = "Удар пневмо-поршня з колосальною кінетичною силою. Відкидає та оглушує.",
        apCost = 2,
        range = 1,
        baseDamage = 22,
        damageType = DamageType.PHYSICAL,
        distanceType = AttackDistanceType.MELEE,
        pushesTarget = true,
        appliesStatus = StatusEffectType.STUNNED,
        statusTurns = 1
    )

    val steamVent = CombatSkill(
        id = "skill_steam_vent",
        name = "Викид Пекельної Пари",
        description = "Вивільняє перегріту пару з реактора на всі сусідні клітинки по колу (3x3).",
        apCost = 2,
        range = 1,
        baseDamage = 14,
        damageType = DamageType.FIRE,
        distanceType = AttackDistanceType.AREA,
        isAreaEffect = true,
        areaRadius = 1,
        appliesStatus = StatusEffectType.BURNING,
        statusTurns = 2
    )

    val bloodLance = CombatSkill(
        id = "skill_blood_lance",
        name = "Кривавий Спис Пустоти",
        description = "Жертвує 10 ОЗ для пронизливого променя безодні на 4 клітинки вперед.",
        apCost = 2,
        range = 4,
        baseDamage = 30,
        damageType = DamageType.ELDRITCH,
        distanceType = AttackDistanceType.RANGED,
        selfHpCost = 10,
        appliesStatus = StatusEffectType.BLEEDING,
        statusTurns = 3
    )

    val voidGaze = CombatSkill(
        id = "skill_void_gaze",
        name = "Погляд Безодні",
        description = "Паралізує ворога жахом безодні на дистанції до 3 клітинок. Оглушує на 2 ходи.",
        apCost = 1,
        range = 3,
        baseDamage = 5,
        damageType = DamageType.ELDRITCH,
        distanceType = AttackDistanceType.RANGED,
        appliesStatus = StatusEffectType.TERRIFIED,
        statusTurns = 2
    )
}
