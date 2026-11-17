package com.example.guitartools.model

import kotlin.math.sqrt

data class KeyCandidate(
    val correlation: Float,
    val pc: Int,
    val isMinor: Boolean
)

object KeyEstimator {

    val KS_MAJOR = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
    val KS_MINOR = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)

    fun estimateKeys(hist: IntArray): List<KeyCandidate> {
        val h = DoubleArray(12) { hist[it].toDouble() }
        val sumH = h.sum()
        if (sumH == 0.0) return emptyList()

        val meanH = sumH / 12.0
        var varianceH = 0.0
        for (v in h) {
            val diff = v - meanH
            varianceH += diff * diff
        }
        if (varianceH == 0.0) return emptyList()
        val stdH = sqrt(varianceH)

        val results = mutableListOf<KeyCandidate>()

        val profiles = listOf(
            false to KS_MAJOR,
            true to KS_MINOR
        )

        for ((isMinor, prof) in profiles) {
            val meanP = prof.sum() / 12.0
            var varianceP = 0.0
            for (v in prof) {
                val diff = v - meanP
                varianceP += diff * diff
            }
            val stdP = sqrt(varianceP)

            for (pc in 0 until 12) {
                // Correlation with shifted profile by pc
                var cov = 0.0
                for (i in 0 until 12) {
                    val shiftedProfVal = prof[Math.floorMod(i - pc, 12)]
                    cov += (h[i] - meanH) * (shiftedProfVal - meanP)
                }
                val corr = (cov / (stdH * stdP)).toFloat()
                results.add(KeyCandidate(corr, pc, isMinor))
            }
        }

        return results.sortedByDescending { it.correlation }
    }
}
