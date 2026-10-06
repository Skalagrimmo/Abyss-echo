package com.example.model

enum class TileType(
    val displayName: String,
    val isWalkable: Boolean,
    val isTransparent: Boolean
) {
    VOID("Порожнеча", false, false),
    WALL("Кам'яні мури", false, false),
    IRON_WALL("Залізна перебірка", false, false),
    FLOOR("Кам'яна плита", true, true),
    RUST_GRATE("Іржава решітка", true, true),
    WATER_PUDDLE("Калюжа води", true, true),
    OIL_SLICK("Пляма мастила", true, true),
    TOXIC_MIRE("Токсичний слиз", true, true),
    EMBER_HEARTH("Вогнище шлаку", true, true),
    CHTHONIC_ALTAR("Хтонічний вівтар", false, true),
    SMELTER_FORGE("Плавильна піч", false, true),
    RELIC_CHEST("Іржава скриня", false, true),
    ELEVATOR_EXIT("Шахта ліфта", true, true)
}

enum class TileElement {
    NONE,
    BURNING,
    ELECTRIFIED,
    POISON_SMOKE,
    CHILLED
}

enum class Faction(
    val title: String,
    val description: String
) {
    IRON_FOUNDRY("Мануфактура Заліза", "Культ шестерень, промислового рабства та незламної сталі."),
    CHTHONIC_ASCETICS("Хтонічні Аскети", "Окультні жерці безодні, що шукають спасіння у плоті та мутаціях."),
    FORGOTTEN_REMNANTS("Забуті Блукачі", "Виживальці та втікачі з глибин, які прагнуть дістатися поверхні.")
}

enum class TimePhase(
    val title: String,
    val dangerLevel: String,
    val modifierDesc: String,
    val ambientR: Float,
    val ambientG: Float,
    val ambientB: Float
) {
    TWILIGHT(
        "Світанок руїн",
        "Помірна",
        "Туман ще розсіяний. Вороги не бачать у темряві.",
        0.35f, 0.35f, 0.45f
    ),
    DEEPENING_GLOOM(
        "Сутінки Безодні",
        "Висока",
        "Безодня згущується. Видимість знижена на 20%, вороги насторожі.",
        0.20f, 0.18f, 0.28f
    ),
    HOUR_OF_WHISPERS(
        "Година Шепоту",
        "Критична",
        "Древні голоси роз'їдають розум. Кожні 20 кроків втрачається глузд.",
        0.14f, 0.10f, 0.22f
    ),
    AWAKENING_OF_GOD(
        "Пробудження Бога Фабрики",
        "Смертельна",
        "Механічний колос відчуває твоє серцебиття. Вороги у люті (+40% сили)!",
        0.28f, 0.08f, 0.08f
    )
}

enum class StatusEffectType(
    val title: String,
    val isHarmful: Boolean
) {
    BLEEDING("Кровотеча", true),
    BURNING("Горіння", true),
    POISONED("Отруєння", true),
    ELECTRIFIED("Шок струмом", true),
    STUNNED("Оглушення", true),
    TERRIFIED("Жах", true),
    ADRENALINE("Овердрайв", false),
    IRON_SKIN("Залізний панцир", false),
    SIPHON_HEAL("Пожива плоті", false)
}

enum class DamageType {
    PHYSICAL,
    FIRE,
    ELECTRIC,
    CORROSION,
    ELDRITCH
}

enum class MutationCategory(val title: String) {
    CHTHONIC_FLESH("Хтонічна Плоть"),
    IRON_CYBERNETICS("Залізна Механіка"),
    OCCULT_SIGIL("Окультні Гліфи")
}

enum class MutationSlot(val title: String) {
    HEAD("Череп / Сенсори"),
    ARMS("Кінцівки / Зброя"),
    TORSO("Торс / Оболонка"),
    LEGS("Опори / Локомоція"),
    CORE("Серце / Джерело")
}

enum class Archetype(
    val title: String,
    val description: String,
    val hp: Int,
    val sanity: Int,
    val scrap: Int,
    val biomass: Int
) {
    IRON_DESERTER(
        "Залізний Дезертир",
        "Колишній оператор плавильної печі. Має важку броню, гідравлічний молот та імунітет до опіків.",
        90, 60, 25, 5
    ),
    CHTHONIC_PENITENT(
        "Хтонічний Каяник",
        "Аскет, заражений спорами безодні. Поглинає плоть ворогів для загоєння ран, але балансує на межі божевілля.",
        75, 45, 5, 25
    ),
    CHRONO_OUTCAST(
        "Хроно-Вигнанець",
        "Тіньовий мандрівник, який вміє сповільнювати час коштом власної крові та має високу спритність.",
        65, 80, 15, 15
    )
}
