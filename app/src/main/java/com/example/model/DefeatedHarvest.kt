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
    val category: MutationCategory = MutationCategory.CHTHONIC_FLESH
)

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
                        title = "Прищепити токсичні протоки",
                        subtitle = "+3 до Атаки. Кожен критичний випад тепер накладає отруєння на жертву.",
                        bonusAttackPower = 3,
                        corruptionDelta = 6,
                        passiveTrait = "Отруєння при критах",
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.CHTHONIC_FLESH
                    ),
                    HarvestMutationOption(
                        title = "Хітинові нарости гомілок",
                        subtitle = "+15 до Макс. ОЗ та +1 до Броні. Шкіра ніг твердне як корунд.",
                        bonusMaxHp = 15,
                        bonusArmor = 1,
                        corruptionDelta = 4,
                        slot = MutationSlot.LEGS,
                        category = MutationCategory.CHTHONIC_FLESH
                    ),
                    HarvestMutationOption(
                        title = "Дистилювати антидот",
                        subtitle = "Відновити +25 ОЗ та очистити -5% Спотворення замість мутації.",
                        healHp = 25,
                        corruptionDelta = -5,
                        bonusBiomass = 8
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
                        title = "Вживити Око Пустоти",
                        subtitle = "+2 до Поля зору, +4 до Атаки. Розблоковує здатність [Погляд Безодні].",
                        bonusFov = 2,
                        bonusAttackPower = 4,
                        corruptionDelta = 12,
                        sanityDelta = -10,
                        grantedSkill = SkillCatalog.voidGaze,
                        slot = MutationSlot.HEAD,
                        category = MutationCategory.CHTHONIC_FLESH
                    ),
                    HarvestMutationOption(
                        title = "Сплести фіброзні щупальця",
                        subtitle = "+10 до Макс. ОЗ. Розблоковує здатність [Хтонічний Хлист] з крадіжкою здоров'я.",
                        bonusMaxHp = 10,
                        corruptionDelta = 8,
                        grantedSkill = SkillCatalog.siphonLash,
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.CHTHONIC_FLESH
                    ),
                    HarvestMutationOption(
                        title = "Розтерти на окультну есенцію",
                        subtitle = "+20 Біомаси, +10 Есенції та відновлення +20 Глузду.",
                        sanityDelta = 20,
                        bonusBiomass = 20
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
                        title = "Вмонтувати поршневий кулак",
                        subtitle = "+6 до Атаки. Розблоковує здатність [Гідравлічний Удар] (відкидання та оглушення).",
                        bonusAttackPower = 6,
                        corruptionDelta = 4,
                        grantedSkill = SkillCatalog.pistonSlam,
                        slot = MutationSlot.ARMS,
                        category = MutationCategory.IRON_CYBERNETICS
                    ),
                    HarvestMutationOption(
                        title = "Титанове бронювання торса",
                        subtitle = "+3 до Броні та +25 до Макс. ОЗ. Поглинає прямі фізичні удари.",
                        bonusArmor = 3,
                        bonusMaxHp = 25,
                        corruptionDelta = 5,
                        slot = MutationSlot.TORSO,
                        category = MutationCategory.IRON_CYBERNETICS
                    ),
                    HarvestMutationOption(
                        title = "Розібрати на високоякісний брухт",
                        subtitle = "+35 Брухту для створення польових інструментів та палива.",
                        bonusScrap = 35
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
                        subtitle = "+15 до Макс. ОЗ, повний імунітет до опіків, розблоковує [Викид Пекельної Пари].",
                        bonusMaxHp = 15,
                        corruptionDelta = 8,
                        grantedSkill = SkillCatalog.steamVent,
                        slot = MutationSlot.CORE,
                        category = MutationCategory.IRON_CYBERNETICS
                    ),
                    HarvestMutationOption(
                        title = "Хроно-пружинні сухожилля",
                        subtitle = "+1 ОД кожен другий хід. Блискавичне маневрування у бою.",
                        bonusAp = 1,
                        corruptionDelta = 6,
                        slot = MutationSlot.LEGS,
                        category = MutationCategory.IRON_CYBERNETICS
                    ),
                    HarvestMutationOption(
                        title = "Очистити пальне для факела",
                        subtitle = "+40 ходів смолоскипа та +15 ОЗ від зігріваючого тепла.",
                        healHp = 15,
                        bonusScrap = 15
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
                        title = "Абсолютна Біомеханічна Еволюція",
                        subtitle = "+40 до Макс. ОЗ, +4 до Броні, +8 до Атаки та +1 постійне Очко Дій (ОД).",
                        bonusMaxHp = 40,
                        bonusArmor = 4,
                        bonusAttackPower = 8,
                        bonusAp = 1,
                        corruptionDelta = 15,
                        slot = MutationSlot.CORE,
                        category = MutationCategory.IRON_CYBERNETICS
                    ),
                    HarvestMutationOption(
                        title = "Криваве Переродження Шантає",
                        subtitle = "+50 до Макс. ОЗ, повне відновлення, розблоковує [Кривавий Спис Пустоти].",
                        bonusMaxHp = 50,
                        healHp = 100,
                        grantedSkill = SkillCatalog.bloodLance,
                        corruptionDelta = 20,
                        sanityDelta = -15,
                        slot = MutationSlot.CORE,
                        category = MutationCategory.OCCULT_SIGIL
                    )
                )
            )
        }
    }
}
