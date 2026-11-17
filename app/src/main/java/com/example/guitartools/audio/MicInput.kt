package com.example.guitartools.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.ArrayDeque
import kotlin.concurrent.thread

class MicInput(
    val sampleRate: Int = 44100,
    val chunk: Int = 2048,
    val maxWindow: Int = 16384
) {
    private val buffer = ArrayDeque<Float>(maxWindow)
    private val lock = Any()
    @Volatile var isRunning = false
        private set

    var errorMessage: String? = null
        private set

    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning) return true
        try {
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuf, chunk * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                errorMessage = "Impossibile inizializzare il microfono"
                return false
            }

            audioRecord?.startRecording()
            isRunning = true

            thread(isDaemon = true, name = "MicRecordThread") {
                val raw = ShortArray(chunk)
                while (isRunning) {
                    val read = audioRecord?.read(raw, 0, chunk) ?: -1
                    if (read > 0) {
                        synchronized(lock) {
                            for (i in 0 until read) {
                                if (buffer.size >= maxWindow) {
                                    buffer.removeFirst()
                                }
                                buffer.addLast(raw[i] / 32768.0f)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Errore microfono"
            isRunning = false
            return false
        }
    }

    fun snapshot(n: Int): FloatArray? {
        synchronized(lock) {
            if (buffer.size < n) return null
            val result = FloatArray(n)
            val iterator = buffer.iterator()
            val skip = buffer.size - n
            var index = 0
            var count = 0
            while (iterator.hasNext()) {
                val valFloat = iterator.next()
                if (count >= skip) {
                    result[index++] = valFloat
                }
                count++
            }
            return result
        }
    }

    fun stop() {
        isRunning = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }
}
