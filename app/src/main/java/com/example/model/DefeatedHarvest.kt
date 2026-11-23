package com.example.model

data class HarvestMutationOption(
    val title: String,
    val subtitle: String,
    val bonusMaxHp: Int = 0,
    val bonusArmor: Int = 0,
    val bonusAttackPower: Int = 0,
    val bonusAp: Int = 0,
    val bonusFov: Int = 0,
    val corruptionDelta: Int = 0,
    val sanityDelta: Int = 0,
    val healHp: Int = 0,
    val bonusScrap: Int = 0,
    val bonusBiomass: Int = 0,
    val grantedSkill: CombatSkill? = null,
    val passiveTrait: String? = null,
    val slot: MutationSlot = MutationSlot.CORE,
    val category: MutationCategory = MutationCategory.CHTHONIC_FLESH,
    val trait: EvolutionTrait = EvolutionTrait.NONE,
    val isResourceSalvage: Boolean = false
)

data class HarvestedOrgan(
    val id: String,
    val name: String,
    val subtitle: String,
    val enemyTitle: String,
    val sourceArchetype: EnemyArchetype,
    val slot: MutationSlot,
    val category: MutationCategory,
    val trait: EvolutionTrait,
    val bonusMaxHp: Int = 0,
    val bonusArmor: Int = 0,
    val bonusAttackPower: Int = 0,
    val bonusAp: Int = 0,
    val bonusFov: Int = 0,
    val corruptionDelta: Int = 0,
    val sanityDelta: Int = 0,
    val grantedSkill: CombatSkill? = null,
    val passiveTrait: String? = null,
    val salvageBiomass: Int = 10,
    val salvageScrap: Int = 5
) {
    fun toMutation(): Mutation = Mutation(
        id = "organ_${trait.name.lowercase()}_${slot.name.lowercase()}",
        name = name,
        description = subtitle,
        category = category,
        slot = slot,
        tier = 1,
        biomassCost = 0,
        scrapCost = 0,
        sanityCost = 0,
        corruptionIncrease = corruptionDelta.coerceAtLeast(0),
        bonusMaxHp = bonusMaxHp,
        bonusArmor = bonusArmor,
        bonusAttackPower = bonusAttackPower,
        bonusAp = bonusAp,
        bonusFov = bonusFov,
        grantedSkillId = grantedSkill?.id,
        passiveDescription = passiveTrait ?: trait.passiveSummary,
        isAcquired = true,
        trait = trait,
        sourceArchetype = sourceArchetype
    )
}

data class DefeatedHarvest(
    val enemyTitle: String,
    val organName: String,
    val organDescription: String,
    val enemyArchetype: EnemyArchetype,
    val options: List<HarvestMutationOption>
)

object HarvestCatalog {
    fun generateHarvestFor(archetype: EnemyArchetype): DefeatedHarvest {
        return when (archetype) {
            EnemyArchetype.CRAWLING_SPIDER -> DefeatedHarvest(
                enemyTitle = "Повзуча Спороноска",
                organName = "Отруйна Залоза Спороноски",
                organDescription = "Пульсуючий біо-резервуар, наповнений концентрованим кислотним секретом.",
                enemyArchetype = archetype,
                options = listOf(
                    HarvestMutationOption(
                        title = "Токсичні протоки Спороноски",
                        subtitle = "+3 до Атаки. Кожен критичний випад накладає отруєння на жертву.",
                        bonusAttackPower = 3,
                        corruptionDelta = 6,
                        passiveTrait = "Отруєння при критах",
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.CHTHONIC_FLESH,
                        trait = EvolutionTrait.TOXIC_CRIT
                    ),
                    HarvestMutationOption(
                        title = "Хітинові нарости гомілок",
                        subtitle = "+15 до Макс. ОЗ та +1 до Броні. Відбиває ближні атаки шипами.",
                        bonusMaxHp = 15,
                        bonusArmor = 1,
                        corruptionDelta = 4,
                        passiveTrait = "Хітинові шипи (3/5/8 ур.)",
                        slot = MutationSlot.LEGS,
                        category = MutationCategory.CHTHONIC_FLESH,
                        trait = EvolutionTrait.CHITIN_THORNS
                    ),
                    HarvestMutationOption(
                        title = "Дистилювати антидот",
                        subtitle = "Відновити +25 ОЗ, +8 Біомаси та очистити -5% Спотворення.",
                        healHp = 25,
                        corruptionDelta = -5,
                        bonusBiomass = 8,
                        isResourceSalvage = true
                    )
                )
            )
            EnemyArchetype.CHTHONIC_CULTIST -> DefeatedHarvest(
                enemyTitle = "Хтонічний Жрець",
                organName = "Окультне Око Безодні",
                organDescription = "Вирване третє око аскета, що продовжує безшумно споглядати тканину часу.",
                enemyArchetype = archetype,
                options = listOf(
                    HarvestMutationOption(
                        title = "Око Пустоти Аскета",
                        subtitle = "+2 до Огляду, +4 до Атаки. Шанс криту зростає у темряві; відкриває [Погляд Безодні].",
                        bonusFov = 2,
                        bonusAttackPower = 4,
                        corruptionDelta = 12,
                        sanityDelta = -10,
                        grantedSkill = SkillCatalog.voidGaze,
                        passiveTrait = "+20%/+30%/+40% криту в темряві",
                        slot = MutationSlot.HEAD,
                        category = MutationCategory.OCCULT_SIGIL,
                        trait = EvolutionTrait.VOID_CRIT_DARKNESS
                    ),
                    HarvestMutationOption(
                        title = "Фіброзні щупальця-поглиначі",
                        subtitle = "+10 до Макс. ОЗ. Критичні удари лікують ОЗ; відкриває [Хтонічний Хлист].",
                        bonusMaxHp = 10,
                        corruptionDelta = 8,
                        grantedSkill = SkillCatalog.siphonLash,
                        passiveTrait = "Вампіризм +4/+6/+8 ОЗ при криті",
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.CHTHONIC_FLESH,
                        trait = EvolutionTrait.SIPHON_CRIT
                    ),
                    HarvestMutationOption(
                        title = "Розтерти на окультну есенцію",
                        subtitle = "+20 Біомаси та відновлення +20 Глузду.",
                        sanityDelta = 20,
                        bonusBiomass = 20,
                        isResourceSalvage = true
                    )
                )
            )
            EnemyArchetype.IRON_AUTOMATON -> DefeatedHarvest(
                enemyTitle = "Фабричний Коваль-Автомат",
                organName = "Гідравлічний Сервопоршень",
                organDescription = "Масивний залізничний механізм з титановим штоком і мастильними клапанами.",
                enemyArchetype = archetype,
                options = listOf(
                    HarvestMutationOption(
                        title = "Поршневий кулак Автомата",
                        subtitle = "+6 до Атаки. Ближні удари відкидають ворогів; відкриває [Гідравлічний Удар].",
                        bonusAttackPower = 6,
                        corruptionDelta = 4,
                        grantedSkill = SkillCatalog.pistonSlam,
                        passiveTrait = "Відкидання та оглушення об стіни",
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.IRON_CYBERNETICS,
                        trait = EvolutionTrait.PISTON_KNOCKBACK
                    ),
                    HarvestMutationOption(
                        title = "Титановий панцир Коваля",
                        subtitle = "+3 до Броні та +25 до Макс. ОЗ. Знижує вхідний фізичний урон.",
                        bonusArmor = 3,
                        bonusMaxHp = 25,
                        corruptionDelta = 5,
                        passiveTrait = "Зниження фізичної шкоди на 3/5/7",
                        slot = MutationSlot.TORSO,
                        category = MutationCategory.IRON_CYBERNETICS,
                        trait = EvolutionTrait.IRON_MITIGATION
                    ),
                    HarvestMutationOption(
                        title = "Розібрати на високоякісний брухт",
                        subtitle = "+35 Брухту для створення польових інструментів та палива.",
                        bonusScrap = 35,
                        isResourceSalvage = true
                    )
                )
            )
            EnemyArchetype.SLAG_HOUND -> DefeatedHarvest(
                enemyTitle = "Шлаковий Гончак",
                organName = "Шлакове Серце-Топка",
                organDescription = "Керамічна капсула з киплячим чавуном, що нагнітає пекельний тиск.",
                enemyArchetype = archetype,
                options = listOf(
                    HarvestMutationOption(
                        title = "Плавильне Серце-Реактор",
                        subtitle = "+15 до Макс. ОЗ, імунітет до опіків і шлаку, відкриває [Викид Пекельної Пари].",
                        bonusMaxHp = 15,
                        corruptionDelta = 8,
                        grantedSkill = SkillCatalog.steamVent,
                        passiveTrait = "Імунітет до вогню та підпал мастила",
                        slot = MutationSlot.CORE,
                        category = MutationCategory.IRON_CYBERNETICS,
                        trait = EvolutionTrait.THERMAL_IMMUNITY
                    ),
                    HarvestMutationOption(
                        title = "Хроно-пружинні сухожилля",
                        subtitle = "+1 ОД кожен другий хід (на III ранзі — щоходу). Блискавичне маневрування.",
                        bonusAp = 1,
                        corruptionDelta = 6,
                        passiveTrait = "Додаткове ОД у бою",
                        slot = MutationSlot.LEGS,
                        category = MutationCategory.IRON_CYBERNETICS,
                        trait = EvolutionTrait.CLOCKWORK_AP
                    ),
                    HarvestMutationOption(
                        title = "Очистити пальне для факела",
                        subtitle = "+15 Брухту та +15 ОЗ від зігріваючого тепла.",
                        healHp = 15,
                        bonusScrap = 15,
                        isResourceSalvage = true
                    )
                )
            )
            EnemyArchetype.CRUCIBLE_TITAN -> DefeatedHarvest(
                enemyTitle = "Титан Горнила",
                organName = "Колос-Ядро Бога Фабрики",
                organDescription = "Вічне джерело сили Мануфактури, де сплавлені тисячі душ і метал глибин.",
                enemyArchetype = archetype,
                options = listOf(
                    HarvestMutationOption(
                        title = "Серце Колоса Горнила",
                        subtitle = "+40 до Макс. ОЗ, +4 до Броні, +8 до Атаки, +1 ОД та регенерація ОЗ щоходу.",
                        bonusMaxHp = 40,
                        bonusArmor = 4,
                        bonusAttackPower = 8,
                        bonusAp = 1,
                        corruptionDelta = 15,
                        passiveTrait = "Регенерація +2/+4/+6 ОЗ щоходу",
                        slot = MutationSlot.CORE,
                        category = MutationCategory.IRON_CYBERNETICS,
                        trait = EvolutionTrait.TITAN_CORE
                    ),
                    HarvestMutationOption(
                        title = "Кривавий Знак Титана",
                        subtitle = "+50 до Макс. ОЗ, +100 ОЗ, бонус урону при низькому ОЗ, відкриває [Кривавий Спис].",
                        bonusMaxHp = 50,
                        healHp = 100,
                        grantedSkill = SkillCatalog.bloodLance,
                        corruptionDelta = 20,
                        sanityDelta = -15,
                        passiveTrait = "Кривавий шал (до +100% урону)",
                        slot = MutationSlot.CORE,
                        category = MutationCategory.OCCULT_SIGIL,
                        trait = EvolutionTrait.BLOOD_FRENZY
                    )
                )
            )
        }
    }
}
