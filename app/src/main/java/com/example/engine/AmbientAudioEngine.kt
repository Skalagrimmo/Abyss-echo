package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.AmbientTrackProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Pure-Kotlin 16-bit PCM synthesizer generating dark atmospheric ambient drones
 * without a rigid rhythmic beat grid. Each biomechanical tier (Flesh, Steel, Crucible)
 * uses phase-continuous oscillators, beating detuned frequencies, LFO sweeps, and
 * filtered noise textures, plus a smoothly blended combat intensity layer.
 */
class AmbientDroneSynthesizer(
    private val sampleRate: Int = 22050
) {
    private var targetProfile: AmbientTrackProfile = AmbientTrackProfile.VOID_MENU
    private var previousProfile: AmbientTrackProfile = AmbientTrackProfile.VOID_MENU
    private var crossfadeProgress: Float = 1f
    private val crossfadeStep: Float = 1f / (sampleRate * 2.0f) // ~2.0s smooth crossfade

    private var targetCombatIntensity: Float = 0f
    private var smoothedCombatIntensity: Float = 0f
    private val intensityStep: Float = 1f / (sampleRate * 1.2f)

    private var targetVolume: Float = 0.75f
    private var smoothedVolume: Float = 0.75f
    private var isMuted: Boolean = false
    private var isPaused: Boolean = false
    private var lifecycleGain: Float = 1f

    // Continuous oscillator phases (radians or normalized cycles) to prevent clicks across buffers
    private var phaseSub = 0.0
    private var phaseDetune = 0.0
    private var phaseHarmonicA = 0.0
    private var phaseHarmonicB = 0.0
    private var phaseLfoSlow = 0.0
    private var phaseLfoBreath = 0.0
    private var phaseTremolo = 0.0

    // Deterministic LCG + low-pass state for filtered wind/steam/furnace noise
    private var noiseSeed: Long = 0x5DEECE66DL
    private var lpNoiseState: Double = 0.0
    private var crackleEnvelope: Double = 0.0

    val currentProfile: AmbientTrackProfile get() = targetProfile
    val currentCrossfadeProgress: Float get() = crossfadeProgress
    val currentSmoothedIntensity: Float get() = smoothedCombatIntensity

    fun setProfile(profile: AmbientTrackProfile) {
        if (profile != targetProfile) {
            previousProfile = targetProfile
            targetProfile = profile
            crossfadeProgress = 0f
        }
    }

    fun setCombatIntensity(intensity: Float) {
        targetCombatIntensity = intensity.coerceIn(0f, 1f)
    }

    fun setMusicVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    fun renderBuffer(out: ShortArray) {
        val twoPi = 2.0 * PI
        val invSampleRate = 1.0 / sampleRate

        for (i in out.indices) {
            // Smooth crossfade progress
            if (crossfadeProgress < 1f) {
                crossfadeProgress = (crossfadeProgress + crossfadeStep).coerceAtMost(1f)
            }

            // Smooth combat intensity tracking
            val intensityDiff = targetCombatIntensity - smoothedCombatIntensity
            smoothedCombatIntensity += intensityDiff.coerceIn(-intensityStep, intensityStep)

            // Smooth volume and lifecycle envelope
            val effTargetVol = if (isMuted || isPaused) 0f else targetVolume
            val volDiff = effTargetVol - smoothedVolume
            smoothedVolume += volDiff * 0.002f

            val targetGain = if (isPaused) 0f else 1f
            lifecycleGain += (targetGain - lifecycleGain) * 0.004f

            // Advance noise generator + simple 1-pole low-pass filter
            noiseSeed = (noiseSeed * 6364136223846793005L + 1442695040888963407L)
            val whiteNoise = ((noiseSeed ushr 33).toInt() / Int.MAX_VALUE.toDouble())
            lpNoiseState = lpNoiseState * 0.94 + whiteNoise * 0.06

            // Rare non-periodic furnace/flesh crackle
            if ((noiseSeed ushr 48) == 0L) {
                crackleEnvelope = 0.85
            }
            crackleEnvelope *= 0.996

            // Base shared slow LFOs
            phaseLfoSlow = (phaseLfoSlow + twoPi * 0.09 * invSampleRate) % twoPi
            phaseLfoBreath = (phaseLfoBreath + twoPi * 0.17 * invSampleRate) % twoPi
            phaseTremolo = (phaseTremolo + twoPi * (2.6 + smoothedCombatIntensity * 2.8) * invSampleRate) % twoPi

            val intensity = smoothedCombatIntensity.toDouble()

            val sampleDouble = if (crossfadeProgress >= 0.9999f) {
                synthesizeProfileSample(targetProfile, intensity, invSampleRate, twoPi)
            } else {
                // Equal-power / smooth cosine crossfade between previous & new tier
                val fadeOut = cos(crossfadeProgress * PI * 0.5)
                val fadeIn = sin(crossfadeProgress * PI * 0.5)
                val sPrev = synthesizeProfileSample(previousProfile, intensity, invSampleRate, twoPi)
                val sNext = synthesizeProfileSample(targetProfile, intensity, invSampleRate, twoPi)
                sPrev * fadeOut + sNext * fadeIn
            }

            // Soft limiter + 16-bit subtle retro quantization warmth
            val scaled = tanh(sampleDouble * 1.15) * smoothedVolume * lifecycleGain * 13500.0
            // 16-bit PCM quantization with faint 12-bit grit during intense crucible/steel swells
            val rawInt = scaled.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i] = rawInt.toShort()
        }
    }

    private fun synthesizeProfileSample(
        profile: AmbientTrackProfile,
        combat: Double,
        invSampleRate: Double,
        twoPi: Double
    ): Double {
        return when (profile) {
            AmbientTrackProfile.VOID_MENU -> {
                // Sparse cold A1 drone (55 Hz) + fifth harmonic shimmer (82.5 Hz)
                phaseSub = (phaseSub + twoPi * 55.0 * invSampleRate) % twoPi
                phaseDetune = (phaseDetune + twoPi * 55.28 * invSampleRate) % twoPi
                phaseHarmonicA = (phaseHarmonicA + twoPi * 82.5 * invSampleRate) % twoPi

                val breath = 0.55 + 0.45 * sin(phaseLfoSlow)
                val sub = (sin(phaseSub) * 0.55 + sin(phaseDetune) * 0.35)
                val fifth = sin(phaseHarmonicA) * (0.14 + 0.10 * sin(phaseLfoBreath))
                val air = lpNoiseState * 0.16
                (sub * breath + fifth + air) * 0.72
            }

            AmbientTrackProfile.FLESH_CATACOMBS -> {
                // Organic G1 sub-bass (48.99 Hz) with visceral beating + respiratory LFO + minor-2nd tension
                val lfoBreath = 0.5 + 0.5 * sin(phaseLfoBreath) // respiratory cycle
                val freqBase = 48.99 + sin(phaseLfoSlow) * 0.35
                phaseSub = (phaseSub + twoPi * freqBase * invSampleRate) % twoPi
                phaseDetune = (phaseDetune + twoPi * (freqBase + 0.42 + combat * 0.45) * invSampleRate) % twoPi
                phaseHarmonicA = (phaseHarmonicA + twoPi * (freqBase * 1.5) * invSampleRate) % twoPi
                // Minor second dissonance (~103.8 Hz G#2) rising with combat tension
                phaseHarmonicB = (phaseHarmonicB + twoPi * 103.83 * invSampleRate) % twoPi

                val visceralPulse = 0.65 + 0.35 * lfoBreath
                val combatTrem = 1.0 - combat * 0.35 * (0.5 + 0.5 * sin(phaseTremolo))
                val organicDrone = (sin(phaseSub) * 0.52 + sin(phaseDetune) * 0.36 + sin(phaseSub * 2.0) * 0.12)
                val wetMurmur = lpNoiseState * (0.22 + 0.25 * lfoBreath)
                val tensionLayer = (sin(phaseHarmonicA) * 0.16 + sin(phaseHarmonicB) * (0.04 + 0.30 * combat))

                (organicDrone * visceralPulse * combatTrem + tensionLayer + wetMurmur) * 0.85
            }

            AmbientTrackProfile.STEEL_FOUNDRY -> {
                // Industrial C2 drone (65.41 Hz) + inharmonic iron-pipe ratios + steam turbine swell
                phaseSub = (phaseSub + twoPi * 65.41 * invSampleRate) % twoPi
                phaseDetune = (phaseDetune + twoPi * 92.50 * invSampleRate) % twoPi // ~sqrt(2) pipe mode
                phaseHarmonicA = (phaseHarmonicA + twoPi * 180.3 * invSampleRate) % twoPi // metallic overtone
                phaseHarmonicB = (phaseHarmonicB + twoPi * (278.1 + sin(phaseLfoSlow) * 1.8) * invSampleRate) % twoPi

                val turbineDrift = 0.6 + 0.4 * cos(phaseLfoSlow)
                // Slight triangle-like odd-harmonic edge for industrial brassiness
                val ironHum = tanh(sin(phaseSub) * 1.6) * 0.46 + sin(phaseDetune) * 0.26
                val pipeResonance = sin(phaseHarmonicA) * (0.12 + 0.20 * combat) * turbineDrift
                val turbineWhine = sin(phaseHarmonicB) * (0.03 + 0.22 * combat) * (0.65 + 0.35 * sin(phaseTremolo))
                val steamHiss = lpNoiseState * (0.28 + 0.24 * combat)

                (ironHum + pipeResonance + turbineWhine + steamHiss) * 0.82
            }

            AmbientTrackProfile.CRUCIBLE_ABYSS -> {
                // Abyssal E1 sub-drone (41.20 Hz) + tritone Bb1 (58.27 Hz) + eldritch choir + ember crackle
                phaseSub = (phaseSub + twoPi * 41.20 * invSampleRate) % twoPi
                phaseDetune = (phaseDetune + twoPi * 58.27 * invSampleRate) % twoPi // tritone
                phaseHarmonicA = (phaseHarmonicA + twoPi * (123.47 + sin(phaseLfoSlow) * 0.9) * invSampleRate) % twoPi
                phaseHarmonicB = (phaseHarmonicB + twoPi * (164.81 - cos(phaseLfoBreath) * 1.1) * invSampleRate) % twoPi

                val abyssSwell = 0.72 + 0.28 * sin(phaseLfoBreath)
                val core = tanh((sin(phaseSub) * 0.60 + sin(phaseDetune) * (0.28 + 0.28 * combat)) * (1.2 + combat * 0.8))
                val choir = (sin(phaseHarmonicA) * 0.16 + sin(phaseHarmonicB) * (0.10 + 0.22 * combat)) * abyssSwell
                val emberCrackle = lpNoiseState * 0.25 + crackleEnvelope * (0.12 + 0.14 * combat) * sin(phaseHarmonicB * 3.1)

                (core * 0.70 + choir + emberCrackle) * 0.88
            }
        }
    }
}

object AmbientAudioEngine {
    private const val SAMPLE_RATE = 22050
    private const val CHUNK_SAMPLES = 1024

    private val scope = CoroutineScope(Dispatchers.IO)
    private val synthesizer = AmbientDroneSynthesizer(SAMPLE_RATE)

    @Volatile
    private var started = false

    @Volatile
    private var pausedByLifecycle = false

    fun ensureStarted() {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            startStreamingLoop()
        }
    }

    private fun startStreamingLoop() {
        scope.launch {
            var track: AudioTrack? = null
            val buffer = ShortArray(CHUNK_SAMPLES)
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(CHUNK_SAMPLES * 4)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                while (true) {
                    if (pausedByLifecycle) {
                        delay(80)
                        continue
                    }
                    synthesizer.renderBuffer(buffer)
                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Throwable) {
                // Safe fallback in headless JVM tests or restricted containers without audio HAL
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {}
            }
        }
    }

    fun setProfile(profile: AmbientTrackProfile) {
        ensureStarted()
        synthesizer.setProfile(profile)
    }

    fun setCombatIntensity(intensity: Float) {
        synthesizer.setCombatIntensity(intensity)
    }

    fun setMusicVolume(volume: Float) {
        synthesizer.setMusicVolume(volume)
    }

    fun setMuted(muted: Boolean) {
        synthesizer.setMuted(muted)
    }

    fun pause() {
        pausedByLifecycle = true
        synthesizer.setPaused(true)
    }

    fun resume() {
        pausedByLifecycle = false
        synthesizer.setPaused(false)
    }
}
