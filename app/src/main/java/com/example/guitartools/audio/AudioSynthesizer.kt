package com.example.guitartools.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

class AudioSynthesizer(val sampleRate: Int = 44100) {

    private var toneTrack: AudioTrack? = null
    @Volatile var isPlayingTone = false
        private set

    fun playTone(frequency: Double) {
        stopTone()

        val cycles = maxOf(1, (frequency).roundToInt())
        val frames = (cycles * sampleRate / frequency).roundToInt()
        if (frames <= 0) return

        val pcm = ShortArray(frames)
        val harmonics = listOf(
            1.0 to 1.0,
            2.0 to 0.7,
            3.0 to 0.5,
            4.0 to 0.3
        )

        for (i in 0 until frames) {
            val t = i.toDouble() / sampleRate
            var sumWave = 0.0
            for ((mult, amp) in harmonics) {
                sumWave += amp * sin(2.0 * PI * frequency * mult * t)
            }
            val normWave = 0.8 * sumWave / 2.5
            val sampleShort = (normWave.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
            pcm[i] = sampleShort
        }

        val bufferSize = frames * 2

        try {
            val track = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(pcm, 0, pcm.size)
            track.setLoopPoints(0, frames, -1)
            track.play()
            toneTrack = track
            isPlayingTone = true
        } catch (_: Exception) {
            isPlayingTone = false
        }
    }

    fun stopTone() {
        isPlayingTone = false
        try {
            toneTrack?.stop()
            toneTrack?.release()
        } catch (_: Exception) {
        }
        toneTrack = null
    }

    fun playClick(isAccent: Boolean) {
        thread(isDaemon = true) {
            val freq = if (isAccent) 1500.0 else 1000.0
            val durationMs = 45
            val frames = sampleRate * durationMs / 1000
            val pcm = ShortArray(frames)

            for (i in 0 until frames) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 90.0)
                val wave = 0.9 * sin(2.0 * PI * freq * t) * env
                pcm[i] = (wave.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
            }

            try {
                val clickTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                    frames * 2,
                    AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                clickTrack.write(pcm, 0, pcm.size)
                clickTrack.play()
                // Auto cleanup thread
                Thread.sleep(durationMs.toLong() + 50)
                clickTrack.stop()
                clickTrack.release()
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        stopTone()
    }
}
