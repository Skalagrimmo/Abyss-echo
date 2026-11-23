package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object SoundSynthesizer {
    private const val SAMPLE_RATE = 22050
    @Volatile
    var isMuted: Boolean = false
    @Volatile
    var sfxVolume: Float = 0.85f

    private val audioScope = CoroutineScope(Dispatchers.IO)
    private val soundQueue = Channel<ShortArray>(capacity = 4)

    // Precomputed sound buffers - computed ONCE at startup to avoid runtime allocation & trig math
    private val strikeBuffer: ShortArray by lazy { generateStrikeBuffer() }
    private val heavySlamBuffer: ShortArray by lazy { generateHeavySlamBuffer() }
    private val droneBuffer: ShortArray by lazy { generateDroneBuffer() }
    private val chimeBuffer: ShortArray by lazy { generateChimeBuffer() }
    private val stepBuffer: ShortArray by lazy { generateStepBuffer() }

    init {
        // Single background worker loop that handles audio playback without thread creation
        audioScope.launch {
            var activeTrack: AudioTrack? = null
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                activeTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeTrack.play()

                for (buffer in soundQueue) {
                    val gain = sfxVolume.coerceIn(0f, 1f)
                    if (!isMuted && gain > 0.01f) {
                        if (gain >= 0.99f) {
                            activeTrack.write(buffer, 0, buffer.size)
                        } else {
                            val scaled = ShortArray(buffer.size) { idx ->
                                (buffer[idx] * gain).toInt().toShort()
                            }
                            activeTrack.write(scaled, 0, scaled.size)
                        }
                    }
                }
            } catch (_: Throwable) {
                // Fallback for restricted containers or JVM tests
            } finally {
                try {
                    activeTrack?.stop()
                    activeTrack?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun playStrike() {
        if (!isMuted) soundQueue.trySend(strikeBuffer)
    }

    fun playHeavySlam() {
        if (!isMuted) soundQueue.trySend(heavySlamBuffer)
    }

    fun playEldritchDrone() {
        if (!isMuted) soundQueue.trySend(droneBuffer)
    }

    fun playMutationChime() {
        if (!isMuted) soundQueue.trySend(chimeBuffer)
    }

    fun playStep() {
        if (!isMuted) soundQueue.trySend(stepBuffer)
    }

    private fun generateStrikeBuffer(): ShortArray {
        val durationMs = 90
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 420.0 * (1.0 - progress * 0.7)
            val envelope = 1.0 - progress
            val sample = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * envelope * 24000).toInt()
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateHeavySlamBuffer(): ShortArray {
        val durationMs = 200
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = 110.0 * (1.0 - progress * 0.6)
            val envelope = (1.0 - progress) * (1.0 - progress)
            val noise = (Math.random() * 2.0 - 1.0) * 0.25
            val tone = sin(2.0 * PI * freq * i / SAMPLE_RATE) * 0.75
            val sample = ((tone + noise) * envelope * 28000).toInt()
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateDroneBuffer(): ShortArray {
        val durationMs = 350
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq1 = 65.41
            val freq2 = 98.0
            val envelope = sin(PI * progress)
            val tone = (sin(2.0 * PI * freq1 * i / SAMPLE_RATE) * 0.6 +
                    sin(2.0 * PI * freq2 * i / SAMPLE_RATE) * 0.4)
            val sample = (tone * envelope * 20000).toInt()
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateChimeBuffer(): ShortArray {
        val durationMs = 280
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val notes = doubleArrayOf(220.0, 277.18, 329.63, 440.0)
        for (i in 0 until numSamples) {
            val noteIdx = ((i.toDouble() / numSamples) * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val sample = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * envelope * 18000).toInt()
            buffer[i] = sample.toShort()
        }
        return buffer
    }

    private fun generateStepBuffer(): ShortArray {
        val durationMs = 30
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val noise = (Math.random() * 2.0 - 1.0)
            val envelope = 1.0 - progress
            val sample = (noise * envelope * 8000).toInt()
            buffer[i] = sample.toShort()
        }
        return buffer
    }
}
