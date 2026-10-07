package com.example.engine

import com.example.model.*
import kotlin.random.Random

data class EventTriggerDecision(
    val event: MoralDilemma?,
    val chance: Double
)

object RunEventEngine {
    private const val BASE_DILEMMA_CHANCE = 0.08
    private const val DILEMMA_COOLDOWN_TURNS = 8

    fun start(seed: Long): RunState =
        RunState(seed = seed).recordEvent(
            RunEvent(0, 1, RunEventType.RUN_STARTED, "Новий забіг розпочато.")
        )

    fun enterFloor(state: RunState, floor: DungeonFloor): RunState =
        state.copy(currentFloorNumber = floor.floorNumber).recordEvent(
            RunEvent(
                state.turnCount,
                floor.floorNumber,
                RunEventType.FLOOR_ENTERED,
                "Вхід у сектор: " + floor.sectorName + "."
            )
        )

    fun shouldTriggerDilemma(
        state: RunState,
        pending: MoralDilemma?,
        playerSanity: Int,
        maxSanity: Int,
        random: Random
    ): EventTriggerDecision {
        if (pending == null) return EventTriggerDecision(null, 0.0)

        val turnsSinceLast = state.eventHistory
            .lastOrNull { it.type == RunEventType.DILEMMA_TRIGGERED || it.type == RunEventType.DILEMMA_RESOLVED }
            ?.let { state.turnCount - it.turn }
            ?: Int.MAX_VALUE

        if (turnsSinceLast < DILEMMA_COOLDOWN_TURNS) {
            return EventTriggerDecision(null, 0.0)
        }

        val floorPressure = ((state.currentFloorNumber - 1) * 0.02).coerceAtMost(0.06)
        val phasePressure = when (state.timePhase) {
            TimePhase.TWILIGHT -> 0.0
            TimePhase.DEEPENING_GLOOM -> 0.02
            TimePhase.HOUR_OF_WHISPERS -> 0.04
            TimePhase.AWAKENING_OF_GOD -> 0.06
        }
        val sanityPressure =
            if (maxSanity > 0 && playerSanity.toDouble() / maxSanity < 0.35) 0.025 else 0.0

        val chance = (BASE_DILEMMA_CHANCE + floorPressure + phasePressure + sanityPressure)
            .coerceAtMost(0.20)

        return if (random.nextDouble() < chance) {
            EventTriggerDecision(pending, chance)
        } else {
            EventTriggerDecision(null, chance)
        }
    }

    fun triggerDilemma(state: RunState, dilemma: MoralDilemma): RunState =
        state.recordEvent(
            RunEvent(
                state.turnCount,
                state.currentFloorNumber,
                RunEventType.DILEMMA_TRIGGERED,
                dilemma.title
            )
        )

    fun resolveDilemma(state: RunState, choice: DilemmaChoice): RunState =
        state.recordEvent(
            RunEvent(
                state.turnCount,
                state.currentFloorNumber,
                RunEventType.DILEMMA_RESOLVED,
                "Вибір: " + choice.title + "."
            )
        )

    fun advanceTime(state: RunState, nextTurn: Int, nextPhase: TimePhase): RunState {
        val next = state.copy(turnCount = nextTurn, timePhase = nextPhase)
        return if (nextPhase != state.timePhase) {
            next.recordEvent(
                RunEvent(
                    nextTurn,
                    state.currentFloorNumber,
                    RunEventType.PHASE_CHANGED,
                    "Настала фаза: " + nextPhase.title + "."
                )
            )
        } else next
    }

    fun acquireMutation(state: RunState, mutation: Mutation): RunState =
        state.recordEvent(
            RunEvent(
                state.turnCount,
                state.currentFloorNumber,
                RunEventType.MUTATION_ACQUIRED,
                "Мутація: " + mutation.name + "."
            )
        )
}
