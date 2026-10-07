package com.example.model

data class StoryRecord(
    val floor: Int,
    val title: String,
    val choiceMade: String,
    val consequence: String
)

enum class RunEventType {
    RUN_STARTED,
    FLOOR_ENTERED,
    DILEMMA_TRIGGERED,
    DILEMMA_RESOLVED,
    PHASE_CHANGED,
    MUTATION_ACQUIRED
}

data class RunEvent(
    val turn: Int,
    val floor: Int,
    val type: RunEventType,
    val description: String
)

data class RunState(
    val seed: Long = 0L,
    val currentFloorNumber: Int = 1,
    val turnCount: Int = 0,
    val timePhase: TimePhase = TimePhase.TWILIGHT,
    val storyChronicle: List<StoryRecord> = emptyList(),
    val factionReputations: Map<Faction, Int> = mapOf(
        Faction.IRON_FOUNDRY to 0,
        Faction.CHTHONIC_ASCETICS to 0,
        Faction.FORGOTTEN_REMNANTS to 0
    ),
    val eventHistory: List<RunEvent> = emptyList()
) {
    fun recordEvent(event: RunEvent): RunState =
        copy(eventHistory = (eventHistory + event).takeLast(100))
}
