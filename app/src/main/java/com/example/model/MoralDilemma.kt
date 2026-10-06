package com.example.model

data class DilemmaChoice(
    val title: String,
    val description: String,
    val hpChange: Int = 0,
    val sanityChange: Int = 0,
    val corruptionChange: Int = 0,
    val scrapChange: Int = 0,
    val biomassChange: Int = 0,
    val essenceChange: Int = 0,
    val factionAffected: Faction? = null,
    val factionReputationChange: Int = 0,
    val rewardMutationId: String? = null,
    val rewardItemId: String? = null,
    val consequenceStory: String
)

data class MoralDilemma(
    val id: String,
    val title: String,
    val subtitle: String,
    val narrative: String,
    val choices: List<DilemmaChoice>
)

object DilemmaCatalog {
    val dilemmas = listOf(
        MoralDilemma(
            id = "dilemma_starving_miner",
            title = "Попіл у горлі, залізо в зубах",
            subtitle = "Затиснутий у клапані робітник",
            narrative = "З парової шахти долинає придушене скиглення. За іржавою решіткою застряг гірник Мануфактури. Його плоть частково прикипіла до перегрітого поршня. «Благаю... Бог Фабрики під нами... не дай мені згоріти у шлаку!» У його кишені блищить креслення гідравлічного замка, а його тіло повне ще свіжої живої тканини.",
            choices = listOf(
                DilemmaChoice(
                    title = "Вирвати з поршня та перев'язати рани",
                    description = "Витратити 15 ОЗ і порцію медикаментів. Врятувати нещасного.",
                    hpChange = -15,
                    sanityChange = 15,
                    corruptionChange = -5,
                    scrapChange = 10,
                    factionAffected = Faction.FORGOTTEN_REMNANTS,
                    factionReputationChange = 25,
                    consequenceStory = "Ви ризикуєте власним тілом і вириваєте гірника з полум'я. Він тремтячими руками віддає вам ключ від аварійного арсеналу і благословляє ваш шлях. Ваш розум яснішає від людяного вчинку серед цього пекла."
                ),
                DilemmaChoice(
                    title = "Зрізати плоть для біо-трансплантату",
                    description = "Позбавити страждань та вилучити 25 біомаси для власної еволюції.",
                    biomassChange = 25,
                    sanityChange = -20,
                    corruptionChange = 15,
                    rewardMutationId = "mut_siphon_tendrils",
                    consequenceStory = "Холодний ніж завершує агонію. Ви поглинаєте свіжу плоть. Нові фіброзні щупальця проростають крізь ваші суглоби, але останній погляд гірника випалюється у вашій пам'яті кошмаром."
                ),
                DilemmaChoice(
                    title = "Увімкнути плавильний тиск заради брухту",
                    description = "Запустити механізм печі. Отримати 35 брухту, але спалити все живе.",
                    scrapChange = 35,
                    sanityChange = -15,
                    corruptionChange = 10,
                    factionAffected = Faction.IRON_FOUNDRY,
                    factionReputationChange = 20,
                    consequenceStory = "Клапани здригаються з диким гуркотом. Чавунний молот трощить усе на шлак. Ви збираєте цінний метал, але повітря наповнюється запахом горілого білка."
                )
            )
        ),
        MoralDilemma(
            id = "dilemma_chthonic_altar",
            title = "Шепіт у печері: Гухаям Гупта-Мантра",
            subtitle = "Окультний моноліт древніх",
            narrative = "Серед базальтових колон ви височіє чорна стела. Із щілин сочиться холодне синє сяйво, а повітря гуде низьким горловим співучим звуком: «Намах Шантає... Шуньяя Намах...» Це прадавні заклинання забутих часів. Моноліт вимагає або крові за знання безодні, або руйнування заради святої сталі.",
            choices = listOf(
                DilemmaChoice(
                    title = "Впасти на коліна та впустити голос у розум",
                    description = "Прийняти безодню. Отримати мутацію Ока Пустоти коштом 25 Глузду.",
                    sanityChange = -25,
                    corruptionChange = 20,
                    rewardMutationId = "mut_abyssal_eye",
                    factionAffected = Faction.CHTHONIC_ASCETICS,
                    factionReputationChange = 30,
                    consequenceStory = "Темрява вливається у ваші зіниці. Тріскається череп, народжуючи чорне око безодні. Ви бачите крізь граніт і час, але тиша назавжди покинула ваші думки."
                ),
                DilemmaChoice(
                    title = "Розбити моноліт залізним ломом",
                    description = "Зневажити окультних богів. Зібрати 20 благословенного заліза і відновити спокій.",
                    scrapChange = 20,
                    sanityChange = 20,
                    corruptionChange = -10,
                    factionAffected = Faction.CHTHONIC_ASCETICS,
                    factionReputationChange = -25,
                    consequenceStory = "Важкий метал трощить нечестивий камінь. Шепіт захлинається тріском. Душі розсіюються, залишаючи чисті рідкісні сплави. Ваш розум відчуває полегшення від тиші."
                ),
                DilemmaChoice(
                    title = "Покропити жертовник власною кров'ю",
                    description = "Віддати 20 ОЗ, але отримати 15 Есенції душ без осквернення розуму.",
                    hpChange = -20,
                    essenceChange = 15,
                    consequenceStory = "Краплі вашої крові шиплять на символах. Безодня приймає плату і вивергає кристалізовану есенцію душ. Ви слабнете тілом, але тримаєте свій дух непохитним."
                )
            )
        ),
        MoralDilemma(
            id = "dilemma_furnace_god_core",
            title = "Серце Бога Фабрики",
            subtitle = "Гігантський паровий дистилятор",
            narrative = "Перед вами б'ється велетенське металеве серце 16-бітної індустріальної імперії. Поршні заввишки з будинок викачують тепло з надр. Робітничі ланцюги обірвані. Залізний Голос лунає крізь димоходи: «Розірви кайдани, або стань частиною шестерні!»",
            choices = listOf(
                DilemmaChoice(
                    title = "Підкорити власні органи паровому тиску",
                    description = "Імплантувати Плавильне Серце-Реактор. Стати наполовину машиною.",
                    hpChange = 20,
                    scrapChange = -15,
                    corruptionChange = 10,
                    rewardMutationId = "mut_smelter_core",
                    factionAffected = Faction.IRON_FOUNDRY,
                    factionReputationChange = 35,
                    consequenceStory = "Пекельна пара розриває грудину, але замість плоті тепер гуде котел із розплавленим чавуном. Ви більше не знаєте втоми і холоду. Залізо в зубах, попіл у горлі."
                ),
                DilemmaChoice(
                    title = "Підірвати гідравліку заради свободи втікачів",
                    description = "Пошкодити серце. Знизити рівень небезпеки часу і отримати 30 брухту.",
                    scrapChange = 30,
                    sanityChange = 25,
                    corruptionChange = -15,
                    factionAffected = Faction.FORGOTTEN_REMNANTS,
                    factionReputationChange = 40,
                    consequenceStory = "Ви розбиваєте контрольний вентиль. Величезний паровий вибух розриває окови в навколишніх штольнях. Втікачі скандують ваше ім'я у темряві, пробиваючись до виходу."
                )
            )
        )
    )
}
