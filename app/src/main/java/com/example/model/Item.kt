package com.example.model

enum class ItemType {
    CONSUMABLE,
    CRAFTING_COMPONENT,
    RELIC
}

data class Item(
    val id: String,
    val name: String,
    val description: String,
    val type: ItemType,
    val hpRestore: Int = 0,
    val sanityRestore: Int = 0,
    val torchAdd: Int = 0,
    val corruptionCleanse: Int = 0,
    val scrapGain: Int = 0,
    val biomassGain: Int = 0,
    var quantity: Int = 1
)

data class Recipe(
    val id: String,
    val resultItemName: String,
    val description: String,
    val scrapCost: Int,
    val biomassCost: Int,
    val essenceCost: Int = 0,
    val producedItem: Item
)

object ItemCatalog {
    val salve = Item(
        id = "item_bio_salve",
        name = "Біо-мазь з плоті",
        description = "Загоює відкриті рани, зшиваючи тканини чужою біомасою (+25 ОЗ, +5% Спотворення).",
        type = ItemType.CONSUMABLE,
        hpRestore = 25,
        corruptionCleanse = -5,
        quantity = 2
    )

    val torchOil = Item(
        id = "item_torch_oil",
        name = "Шлакова олива",
        description = "Заправляє смолоскип, розганяючи задушливу темряву на 40 ходів.",
        type = ItemType.CONSUMABLE,
        torchAdd = 40,
        quantity = 2
    )

    val smellingSalts = Item(
        id = "item_smelling_salts",
        name = "Сіль тверезості",
        description = "Різкий хімічний дим приводить до тями і тамує шепіт безодні (+30 Глузду).",
        type = ItemType.CONSUMABLE,
        sanityRestore = 30,
        quantity = 1
    )

    val fireFlask = Item(
        id = "item_fire_flask",
        name = "Термітний заряд",
        description = "Вибухова суміш, готова до кидка у натовп потвор.",
        type = ItemType.CONSUMABLE,
        quantity = 1
    )

    val craftRecipes = listOf(
        Recipe(
            id = "recipe_salve",
            resultItemName = "Біо-мазь з плоті",
            description = "Зварити 10 біомаси для загоєння ран.",
            scrapCost = 0,
            biomassCost = 10,
            producedItem = salve.copy(quantity = 1)
        ),
        Recipe(
            id = "recipe_oil",
            resultItemName = "Шлакова олива",
            description = "Очистити залишки мастила та брухту для підтримки вогню.",
            scrapCost = 12,
            biomassCost = 4,
            producedItem = torchOil.copy(quantity = 1)
        ),
        Recipe(
            id = "recipe_salts",
            resultItemName = "Сіль тверезості",
            description = "Викристалізувати окультні залишки проти божевілля.",
            scrapCost = 10,
            biomassCost = 10,
            producedItem = smellingSalts.copy(quantity = 1)
        )
    )
}
