package com.example.guitartools.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PitchDetector {

    fun detectPitch(
        x: FloatArray,
        sr: Int = 44100,
        fmin: Float = 60.0f,
        fmax: Float = 700.0f,
        silenceRms: Float = 0.01f
    ): Float? {
        val n = x.size
        if (n == 0) return null

        // Calcolo DC Offset e sottrazione
        var sum = 0.0f
        var sumSq = 0.0f
        for (i in 0 until n) {
            sum += x[i]
        }
        val mean = sum / n

        val xDC = FloatArray(n)
        for (i in 0 until n) {
            val valCentered = x[i] - mean
            xDC[i] = valCentered
            sumSq += valCentered * valCentered
        }

        val rms = sqrt(sumSq / n)
        if (rms < silenceRms) return null

        // Lag bounds per la ricerca autocorrelazione
        val lo = max(1, (sr / fmax).toInt())
        val hi = min(n - 2, (sr / fmin).toInt())
        if (hi <= lo) return null

        // Calcolo Autocorrelazione limitato ai lag necessari
        var ac0 = 0.0f
        for (i in 0 until n) {
            ac0 += xDC[i] * xDC[i]
        }
        if (ac0 <= 0f) return null

        val ac = FloatArray(hi + 2)
        for (k in lo - 1..hi + 1) {
            var sumK = 0.0f
            for (i in 0 until n - k) {
                sumK += xDC[i] * xDC[i + k]
            }
            ac[k] = sumK / ac0
        }

        // Trova picchi locali nel range [lo, hi]
        val peaks = mutableListOf<Int>()
        for (k in lo..hi) {
            if (ac[k] > ac[k - 1] && ac[k] >= ac[k + 1]) {
                peaks.add(k)
            }
        }
        if (peaks.isEmpty()) return null

        var bestVal = -1.0f
        for (p in peaks) {
            if (ac[p] > bestVal) {
                bestVal = ac[p]
            }
        }
        if (bestVal < 0.35f) return null

        // Seleziona il primo picco che sia almeno il 85% del picco massimo (evita errori di ottava)
        var selectedPeak = peaks[0]
        for (p in peaks) {
            if (ac[p] >= 0.85f * bestVal) {
                selectedPeak = p
                break
            }
        }

        val k = selectedPeak
        val a = ac[k - 1]
        val b = ac[k]
        val c = ac[k + 1]
        val denom = a - 2 * b + c
        val shift = if (denom != 0f) 0.5f * (a - c) / denom else 0.0f

        val period = k + shift
        return if (period > 0) sr / period else null
    }

    fun windowFor(fmin: Float, sampleRate: Int = 44100): Int {
        val calculated = (12 * sampleRate / fmin).toInt()
        return min(16384, max(8192, calculated))
    }
}
