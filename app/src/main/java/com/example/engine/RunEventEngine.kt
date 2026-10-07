package com.example.engine

import com.example.model.*

object RunEventEngine {
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
