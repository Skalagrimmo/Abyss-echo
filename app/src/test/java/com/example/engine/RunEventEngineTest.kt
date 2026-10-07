package com.example.engine

import com.example.model.DilemmaCatalog
import com.example.model.RunEvent
import com.example.model.RunEventType
import com.example.model.TimePhase
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunEventEngineTest {
    private val dilemma = DilemmaCatalog.dilemmas.first()

    @Test
    fun dilemmaChanceIsDeterministicForSameSeed() {
        val state = RunEventEngine.start(123L)
        val first = RunEventEngine.shouldTriggerDilemma(state, dilemma, 70, 70, Random(777L))
        val second = RunEventEngine.shouldTriggerDilemma(state, dilemma, 70, 70, Random(777L))

        assertEquals(first.chance, second.chance, 0.0)
        assertEquals(first.event?.id, second.event?.id)
    }

    @Test
    fun dilemmaCooldownBlocksImmediateRetrigger() {
        val state = RunEventEngine.start(123L)
            .copy(turnCount = 5)
            .recordEvent(RunEvent(5, 1, RunEventType.DILEMMA_TRIGGERED, dilemma.title))

        val decision = RunEventEngine.shouldTriggerDilemma(state, dilemma, 20, 70, Random(1L))

        assertNull(decision.event)
        assertEquals(0.0, decision.chance, 0.0)
    }

    @Test
    fun eventChanceRisesWithDanger() {
        val early = RunEventEngine.start(123L)
        val late = early.copy(currentFloorNumber = 3, timePhase = TimePhase.AWAKENING_OF_GOD)

        val earlyDecision = RunEventEngine.shouldTriggerDilemma(early, dilemma, 70, 70, Random(5L))
        val lateDecision = RunEventEngine.shouldTriggerDilemma(late, dilemma, 70, 70, Random(5L))

        assertEquals(true, lateDecision.chance > earlyDecision.chance)
    }
}
