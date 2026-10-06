package com.example.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object SoundSynthesizer {
    private const val SAMPLE_RATE = 22050
    var isMuted: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playStrike() {
        if (isMuted) return
        scope.launch {
            val durationMs = 120
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 450.0 * (1.0 - progress * 0.7)
                val envelope = 1.0 - progress
                val sample = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * envelope * 28000).toInt()
                buffer[i] = sample.toShort()
            }
            playBuffer(buffer)
        }
    }

    fun playHeavySlam() {
        if (isMuted) return
        scope.launch {
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq = 120.0 * (1.0 - progress * 0.6)
                val envelope = (1.0 - progress) * (1.0 - progress)
                // Add noise
                val noise = (Math.random() * 2.0 - 1.0) * 0.3
                val tone = sin(2.0 * PI * freq * i / SAMPLE_RATE) * 0.7
                val sample = ((tone + noise) * envelope * 31000).toInt()
                buffer[i] = sample.toShort()
            }
            playBuffer(buffer)
        }
    }

    fun playEldritchDrone() {
        if (isMuted) return
        scope.launch {
            val durationMs = 450
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val freq1 = 65.41 // Low C
                val freq2 = 98.0  // G
                val envelope = sin(PI * progress) // smooth fade in and out
                val tone = (sin(2.0 * PI * freq1 * i / SAMPLE_RATE) * 0.6 +
                        sin(2.0 * PI * freq2 * i / SAMPLE_RATE) * 0.4)
                val sample = (tone * envelope * 24000).toInt()
                buffer[i] = sample.toShort()
            }
            playBuffer(buffer)
        }
    }

    fun playMutationChime() {
        if (isMuted) return
        scope.launch {
            val durationMs = 350
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)
            val notes = listOf(220.0, 277.18, 329.63, 440.0) // A minor arpeggio

            for (i in 0 until numSamples) {
                val noteIdx = ((i.toDouble() / numSamples) * notes.size).toInt().coerceIn(0, notes.size - 1)
                val freq = notes[noteIdx]
                val envelope = 1.0 - (i.toDouble() / numSamples)
                val sample = (sin(2.0 * PI * freq * i / SAMPLE_RATE) * envelope * 22000).toInt()
                buffer[i] = sample.toShort()
            }
            playBuffer(buffer)
        }
    }

    fun playStep() {
        if (isMuted) return
        scope.launch {
            val durationMs = 40
            val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val noise = (Math.random() * 2.0 - 1.0)
                val envelope = 1.0 - progress
                val sample = (noise * envelope * 12000).toInt()
                buffer[i] = sample.toShort()
            }
            playBuffer(buffer)
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size * 1000L / SAMPLE_RATE) + 50)
            track.release()
        } catch (_: Exception) {
            // Audio ignore on low-resource container
        }
    }
}
