package com.example.model

enum class EvolutionTrait(
    val title: String,
    val passiveSummary: String
) {
    NONE("Базова адаптація", "Підсилює базові характеристики носія."),
    SIPHON_CRIT("Вампіричні щупальця", "Критичні удари відновлюють ОЗ (+4/+6/+8 за ранг)."),
    TOXIC_CRIT("Токсичні протоки", "Критичні удари та важкі випади отруюють ціль."),
    CHITIN_THORNS("Хітинові шипи", "Атакуючі у ближньому бою отримують шиповий урон (3/5/8)."),
    VOID_CRIT_DARKNESS("Погляд Пустоти", "У темряві або при низькому глузді шанс криту зростає (+20%/+30%/+40%)."),
    TOXIC_STEP("Кислотний слід", "Кроки поруч із ворогами мають шанс накласти Отруєння."),
    PISTON_KNOCKBACK("Гідравлічний імпульс", "Ближні удари відкидають ворога та оглушують об стіну."),
    THERMAL_IMMUNITY("Термічний реактор", "Імунітет до горіння й шлаку; кроки запалюють мастило."),
    IRON_MITIGATION("Титанове бронювання", "Зменшує кожен вхідний фізичний урон на 3/5/7."),
    CLOCKWORK_AP("Хроно-суглоби", "Додатковий ОД кожен другий хід (на III ранзі — щоходу)."),
    BLOOD_FRENZY("Кривавий мантр", "Чим менше ОЗ, тим вищий урон (до +50%/+75%/+100%)."),
    SOUL_SIPHON("Клітка душ", "Кожен знищений ворог повертає 3/5/7 пунктів Глузду."),
    TITAN_CORE("Серце Колоса", "Регенерує 2/4/6 ОЗ щоходу та посилює броню.")
}

data class BranchSynergy(
    val category: MutationCategory,
    val equippedCount: Int,
    val minorActive: Boolean,
    val apexActive: Boolean,
    val title: String,
    val minorDesc: String,
    val apexDesc: String
)

data class Mutation(
    val id: String,
    val name: String,
    val description: String,
    val category: MutationCategory,
    val slot: MutationSlot,
    val tier: Int = 1, // Evolution rank 1..3
    val biomassCost: Int,
    val scrapCost: Int,
    val sanityCost: Int,
    val corruptionIncrease: Int,
    // Base stat modifiers (at Rank I)
    val bonusMaxHp: Int = 0,
    val bonusArmor: Int = 0,
    val bonusAttackPower: Int = 0,
    val bonusAp: Int = 0,
    val bonusFov: Int = 0,
    // Unlocked skill id
    val grantedSkillId: String? = null,
    val passiveDescription: String? = null,
    val isAcquired: Boolean = false,
    val trait: EvolutionTrait = EvolutionTrait.NONE,
    val sourceArchetype: EnemyArchetype? = null
) {
    private fun scaleStat(base: Int): Int {
        if (base == 0) return 0
        return when (tier.coerceIn(1, 3)) {
            1 -> base
            2 -> maxOf(base + 1, (base * 1.5f).toInt())
            else -> base * 2
        }
    }

    fun effectiveMaxHp(): Int = scaleStat(bonusMaxHp)
    fun effectiveArmor(): Int = scaleStat(bonusArmor)
    fun effectiveAttackPower(): Int = scaleStat(bonusAttackPower)
    fun effectiveBonusAp(): Int = if (bonusAp <= 0) 0 else if (tier >= 3) bonusAp + 1 else bonusAp
    fun effectiveFov(): Int = if (bonusFov <= 0) 0 else bonusFov + (tier.coerceIn(1, 3) - 1)

    fun tierRoman(): String = when (tier.coerceIn(1, 3)) {
        1 -> "I"
        2 -> "II"
        else -> "III"
    }
}

object MutationCatalog {
    val allMutations = listOf(
        // Chthonic Flesh branch
        Mutation(
            id = "mut_siphon_tendrils",
            name = "Щупальця-Поглиначі",
            description = "З передпліч проростають окультні фіброзні відростки, здатні висмоктувати життєву силу жертв.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.ARMS,
            tier = 1,
            biomassCost = 15,
            scrapCost = 0,
            sanityCost = 10,
            corruptionIncrease = 12,
            bonusAttackPower = 3,
            grantedSkillId = "skill_siphon_lash",
            passiveDescription = "Кожен критичний удар повертає 4/6/8 ОЗ.",
            trait = EvolutionTrait.SIPHON_CRIT,
            sourceArchetype = EnemyArchetype.CHTHONIC_CULTIST
        ),
        Mutation(
            id = "mut_chitin_carapace",
            name = "Хітиновий Панцир Безодні",
            description = "Шкіра покривається багатошаровими чорними хітиновими наростами, що відбивають удари.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.TORSO,
            tier = 1,
            biomassCost = 20,
            scrapCost = 0,
            sanityCost = 5,
            corruptionIncrease = 10,
            bonusMaxHp = 20,
            bonusArmor = 3,
            passiveDescription = "Вороги отримують 3/5/8 шипового урону при ближній атаці.",
            trait = EvolutionTrait.CHITIN_THORNS,
            sourceArchetype = EnemyArchetype.CRAWLING_SPIDER
        ),
        Mutation(
            id = "mut_abyssal_eye",
            name = "Око Пустоти",
            description = "На лобі відкривається третє чорне око без зіниці, що розрізає темряву та виявляє ворогів крізь стіни.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.HEAD,
            tier = 1,
            biomassCost = 25,
            scrapCost = 0,
            sanityCost = 20,
            corruptionIncrease = 18,
            bonusFov = 3,
            grantedSkillId = "skill_void_gaze",
            passiveDescription = "Шанс критичного удару зростає на +20%/+30%/+40% у темряві.",
            trait = EvolutionTrait.VOID_CRIT_DARKNESS,
            sourceArchetype = EnemyArchetype.CHTHONIC_CULTIST
        ),
        Mutation(
            id = "mut_acid_glands",
            name = "Кислотні Залози",
            description = "М'язи ніг пульсують їдким токсином, залишаючи за собою отруйні сліди при ухилянні.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.LEGS,
            tier = 1,
            biomassCost = 22,
            scrapCost = 0,
            sanityCost = 8,
            corruptionIncrease = 14,
            bonusAttackPower = 2,
            passiveDescription = "Кроки у бою мають шанс отруїти ворога поруч.",
            trait = EvolutionTrait.TOXIC_STEP,
            sourceArchetype = EnemyArchetype.CRAWLING_SPIDER
        ),

        // Iron Cybernetics branch
        Mutation(
            id = "mut_hydraulic_piston",
            name = "Пневматичний Поршень-Кулак",
            description = "Важкий гідравлічний циліндр, імплантований у кістку руки. Завдає нищівного удару з відкиданням.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.ARMS,
            tier = 1,
            biomassCost = 5,
            scrapCost = 20,
            sanityCost = 0,
            corruptionIncrease = 5,
            bonusAttackPower = 5,
            grantedSkillId = "skill_piston_slam",
            passiveDescription = "Ближні удари відкидають ворогів на 1 клітинку і оглушують об стіни.",
            trait = EvolutionTrait.PISTON_KNOCKBACK,
            sourceArchetype = EnemyArchetype.IRON_AUTOMATON
        ),
        Mutation(
            id = "mut_smelter_core",
            name = "Плавильне Серце-Реактор",
            description = "Мініатюрна паливна камера в грудині, що кипить рідким чавуном, підігріваючи кров і випаровуючи слиз.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.CORE,
            tier = 1,
            biomassCost = 5,
            scrapCost = 35,
            sanityCost = 0,
            corruptionIncrease = 8,
            bonusMaxHp = 15,
            bonusAttackPower = 3,
            grantedSkillId = "skill_steam_vent",
            passiveDescription = "Імунітет до опіків та шлаку. Кроки запалюють плями мастила.",
            trait = EvolutionTrait.THERMAL_IMMUNITY,
            sourceArchetype = EnemyArchetype.SLAG_HOUND
        ),
        Mutation(
            id = "mut_plated_exoskeleton",
            name = "Сталевий Екзоскелет",
            description = "Броньовані залізничні ресори, прикручені болтами до хребта й стегон.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.TORSO,
            tier = 1,
            biomassCost = 0,
            scrapCost = 25,
            sanityCost = 0,
            corruptionIncrease = 4,
            bonusArmor = 5,
            passiveDescription = "Знижує вхідний фізичний урон на 3/5/7 одиниць.",
            trait = EvolutionTrait.IRON_MITIGATION,
            sourceArchetype = EnemyArchetype.IRON_AUTOMATON
        ),
        Mutation(
            id = "mut_clockwork_springs",
            name = "Хроно-Пружинні Суглоби",
            description = "Шестерневі механізми в колінах, що накопичують кінетичну енергію для блискавичних випадів.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.LEGS,
            tier = 1,
            biomassCost = 0,
            scrapCost = 28,
            sanityCost = 0,
            corruptionIncrease = 6,
            bonusAp = 1,
            passiveDescription = "+1 додатковий ОД кожен другий хід (на III ранзі — щоходу).",
            trait = EvolutionTrait.CLOCKWORK_AP,
            sourceArchetype = EnemyArchetype.SLAG_HOUND
        ),

        // Occult Sigils branch
        Mutation(
            id = "mut_blood_inscription",
            name = "Кривавий Знак Шантає",
            description = "Викарбуваний на шкірі древній мантр спокою, що перетворює біль на чисту енергію нищення.",
            category = MutationCategory.OCCULT_SIGIL,
            slot = MutationSlot.CORE,
            tier = 1,
            biomassCost = 18,
            scrapCost = 15,
            sanityCost = 15,
            corruptionIncrease = 20,
            bonusAttackPower = 6,
            grantedSkillId = "skill_blood_lance",
            passiveDescription = "Чим менше ОЗ, тим вищий урон (до +50%/+75%/+100%).",
            trait = EvolutionTrait.BLOOD_FRENZY,
            sourceArchetype = EnemyArchetype.CRUCIBLE_TITAN
        ),
        Mutation(
            id = "mut_soul_cage",
            name = "Клітка Душ",
            description = "Окультна срібна діадема, що притягує залишки пам'яті вбитих істот та відновлює розум.",
            category = MutationCategory.OCCULT_SIGIL,
            slot = MutationSlot.HEAD,
            tier = 1,
            biomassCost = 12,
            scrapCost = 20,
            sanityCost = -10,
            corruptionIncrease = 10,
            passiveDescription = "Кожен знищений ворог повертає 3/5/7 пунктів Глузду.",
            trait = EvolutionTrait.SOUL_SIPHON,
            sourceArchetype = EnemyArchetype.CHTHONIC_CULTIST
        )
    )
}
