package com.example.model

data class Mutation(
    val id: String,
    val name: String,
    val description: String,
    val category: MutationCategory,
    val slot: MutationSlot,
    val tier: Int,
    val biomassCost: Int,
    val scrapCost: Int,
    val sanityCost: Int,
    val corruptionIncrease: Int,
    // Stat modifiers
    val bonusMaxHp: Int = 0,
    val bonusArmor: Int = 0,
    val bonusAttackPower: Int = 0,
    val bonusAp: Int = 0,
    val bonusFov: Int = 0,
    // Unlocked skill id
    val grantedSkillId: String? = null,
    val passiveDescription: String? = null,
    val isAcquired: Boolean = false
)

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
            passiveDescription = "Кожен критичний удар повертає 4 ОЗ."
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
            passiveDescription = "Вороги отримують 3 шипового урону при ближній атаці."
        ),
        Mutation(
            id = "mut_abyssal_eye",
            name = "Око Пустоти",
            description = "На лобі відкривається третє чорне око без зіниці, що розрізає темряву та виявляє ворогів крізь стіни.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.HEAD,
            tier = 2,
            biomassCost = 25,
            scrapCost = 0,
            sanityCost = 20,
            corruptionIncrease = 18,
            bonusFov = 3,
            grantedSkillId = "skill_void_gaze",
            passiveDescription = "Шанс критичного удару зростає на +25% у повній темряві."
        ),
        Mutation(
            id = "mut_acid_glands",
            name = "Кислотні Залози",
            description = "М'язи ніг пульсують їдким токсином, залишаючи за собою отруйні сліди при ухилянні.",
            category = MutationCategory.CHTHONIC_FLESH,
            slot = MutationSlot.LEGS,
            tier = 2,
            biomassCost = 22,
            scrapCost = 0,
            sanityCost = 8,
            corruptionIncrease = 14,
            bonusAttackPower = 2,
            passiveDescription = "Кроки у бою мають шанс отруїти ворога поруч."
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
            passiveDescription = "Удари відкидають ворогів на 1 клітинку і оглушують об стіни."
        ),
        Mutation(
            id = "mut_smelter_core",
            name = "Плавильне Серце-Реактор",
            description = "Мініатюрна паливна камера в грудині, що кипить рідким чавуном, підігріваючи кров і випаровуючи слиз.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.CORE,
            tier = 2,
            biomassCost = 5,
            scrapCost = 35,
            sanityCost = 0,
            corruptionIncrease = 8,
            bonusMaxHp = 15,
            bonusAttackPower = 3,
            grantedSkillId = "skill_steam_vent",
            passiveDescription = "Повний імунітет до опіків та заморозки. Кроки запалюють плями мастила."
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
            passiveDescription = "Знижує фізичний урон на 4 одиниці."
        ),
        Mutation(
            id = "mut_clockwork_springs",
            name = "Хроно-Пружинні Суглоби",
            description = "Шестерневі механізми в колінах, що накопичують кінетичну енергію для блискавичних випадів.",
            category = MutationCategory.IRON_CYBERNETICS,
            slot = MutationSlot.LEGS,
            tier = 2,
            biomassCost = 0,
            scrapCost = 28,
            sanityCost = 0,
            corruptionIncrease = 6,
            bonusAp = 1,
            passiveDescription = "+1 додатковий пункт дії (ОД) кожен другий хід."
        ),

        // Occult Sigils branch
        Mutation(
            id = "mut_blood_inscription",
            name = "Кривавий Знак Шантає",
            description = "Викарбуваний на шкірі древній мантр спокою, що перетворює біль на чисту енергію нищення.",
            category = MutationCategory.OCCULT_SIGIL,
            slot = MutationSlot.CORE,
            tier = 2,
            biomassCost = 18,
            scrapCost = 15,
            sanityCost = 15,
            corruptionIncrease = 20,
            bonusAttackPower = 6,
            grantedSkillId = "skill_blood_lance",
            passiveDescription = "Чим менше здоров'я (ОЗ), тим вищий урон (до +50%)."
        ),
        Mutation(
            id = "mut_soul_cage",
            name = "Клітка Душ",
            description = "Окультна срібна діадема, що притягує залишки пам'яті вбитих істот та відновлює розум.",
            category = MutationCategory.OCCULT_SIGIL,
            slot = MutationSlot.HEAD,
            tier = 2,
            biomassCost = 12,
            scrapCost = 20,
            sanityCost = -10,
            corruptionIncrease = 10,
            passiveDescription = "Кожен знищений ворог повертає 3 пункти Глузду."
        )
    )
}
