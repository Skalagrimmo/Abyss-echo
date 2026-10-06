package com.example.model

data class StatusEffect(
    val type: StatusEffectType,
    val durationTurns: Int,
    val power: Int = 1
)
