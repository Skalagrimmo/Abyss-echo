package com.example.model

/**
 * Snapshot of the turn-based tactical grid computed from the player's characteristics
 * (AP, locomotion mutations/archetype, FOV/sensor traits, active statuses),
 * equipped skill, terrain hazards, and visible enemy threat ranges.
 */
data class TacticalGridState(
    val moveCosts: Map<GridPos, Int> = emptyMap(),
    val movePaths: Map<GridPos, List<GridPos>> = emptyMap(),
    val hazardMoveCells: Set<GridPos> = emptySet(),
    val attackCells: Set<GridPos> = emptySet(),
    val aoePreviewCells: Set<GridPos> = emptySet(),
    val enemyThreatCells: Set<GridPos> = emptySet(),
    val canAffordSkill: Boolean = true,
    val effectiveAttackRange: Int = 1,
    val maxMoveSteps: Int = 0,
    val freeStrideRemaining: Int = 0
)

/**
 * Two-tap movement preview state:
 * First tap on a reachable grid cell previews the shortest path and AP cost;
 * Second tap on the same target cell executes step-by-step movement along the route.
 */
data class RoutePreviewState(
    val targetPos: GridPos? = null,
    val path: List<GridPos> = emptyList(),
    val apCost: Int = 0,
    val crossesHazard: Boolean = false,
    val endsInThreatZone: Boolean = false
)

/**
 * Real-time combat damage forecast for the currently selected enemy & skill,
 * mirroring CombatEngine's exact combo, evolution, crit, and armor rules.
 */
data class TargetDamageForecast(
    val enemyId: String,
    val skillId: String,
    val minDamage: Int,
    val maxDamage: Int,
    val critChancePercent: Int,
    val isGuaranteedLethal: Boolean,
    val isPotentialLethal: Boolean,
    val isInRange: Boolean,
    val canAffordAp: Boolean,
    val armorBlocked: Int
)
