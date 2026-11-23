package com.chibi.photo.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

enum class SoundProfile(val label: String) {
    TACTILE_STUDIO("Studio pluck"),
    HYDRO_GEL("Hydro drop"),
    FM_CHIME("FM harmonic")
}

/**
 * Real-time Procedural PCM Audio Synthesizer.
 * Synthesizes multi-timbre tactile grab plucks, poke ripples, and frequency-matched
 * harmonic spring oscillations with zero external audio assets.
 */
class ProceduralAudioEngine {
    private val sampleRate = 44100
    private var isMuted = false
    var activeProfile: SoundProfile = SoundProfile.TACTILE_STUDIO
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun playGrabSound(intensity: Float = 1.0f) {
        if (isMuted) return
        val profile = activeProfile
        scope.launch {
            try {
                val durationSec = 0.085f
                val numSamples = (sampleRate * durationSec).toInt()
                val pcmBuffer = ShortArray(numSamples)

                val (startFreq, endFreq) = when (profile) {
                    SoundProfile.TACTILE_STUDIO -> 210.0 to 142.0
                    SoundProfile.HYDRO_GEL -> 165.0 to 290.0
                    SoundProfile.FM_CHIME -> 320.0 to 240.0
                }

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = startFreq * (endFreq / startFreq).pow(progress)
                    phase += 2.0 * Math.PI * freq / sampleRate

                    val env = if (progress < 0.12) progress / 0.12 else (1.0 - progress).pow(1.8)
                    val overtone = sin(phase * 2.0) * 0.25 * (1.0 - progress)
                    val sample = ((sin(phase) + overtone) * env * intensity * 0.85).coerceIn(-1.0, 1.0)
                    pcmBuffer[i] = (sample * 29000).toInt().toShort()
                }
                playBuffer(pcmBuffer)
            } catch (_: Exception) {}
        }
    }

    fun playReleaseSound(stretchDistance: Float, wobbleFreq: Float = 6.8f) {
        if (isMuted) return
        val profile = activeProfile
        scope.launch {
            try {
                val stretchNorm = (stretchDistance / 110f).coerceIn(0.35f, 1.8f)
                val durationSec = 0.36f
                val numSamples = (sampleRate * durationSec).toInt()
                val pcmBuffer = ShortArray(numSamples)

                val baseFreq = when (profile) {
                    SoundProfile.TACTILE_STUDIO -> 225.0 * (1.0 + (stretchNorm - 1.0) * 0.25)
                    SoundProfile.HYDRO_GEL -> 180.0 * (1.0 + (stretchNorm - 1.0) * 0.35)
                    SoundProfile.FM_CHIME -> 340.0 * (1.0 + (stretchNorm - 1.0) * 0.20)
                }
                val targetFreq = baseFreq * 0.72

                var carrierPhase = 0.0
                var wobblePhase = 0.0

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val pitchWobble = sin(wobblePhase) * 18.0 * exp(-progress * 5.0)
                    val freq = baseFreq * (targetFreq / baseFreq).pow(progress) + pitchWobble

                    carrierPhase += 2.0 * Math.PI * freq / sampleRate
                    wobblePhase += 2.0 * Math.PI * wobbleFreq / sampleRate

                    val decay = exp(-progress * 6.5)
                    val ampMod = 0.68 + 0.32 * cos(wobblePhase)
                    val harmonic = if (profile == SoundProfile.FM_CHIME) {
                        sin(carrierPhase * 1.5) * 0.35
                    } else {
                        sin(carrierPhase * 2.0) * 0.15
                    }

                    val sample = ((sin(carrierPhase) + harmonic) * decay * ampMod * stretchNorm * 0.8).coerceIn(-1.0, 1.0)
                    pcmBuffer[i] = (sample * 31000).toInt().toShort()
                }
                playBuffer(pcmBuffer)
            } catch (_: Exception) {}
        }
    }

    private fun playBuffer(pcm: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()

        scope.launch {
            delay(480)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }
}
