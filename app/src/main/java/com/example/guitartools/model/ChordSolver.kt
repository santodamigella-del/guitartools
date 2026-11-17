package com.example.guitartools.model

import kotlin.math.abs

data class Barre(
    val fret: Int,
    val stringStart: Int,
    val stringEnd: Int
)

data class Voicing(
    val frets: List<Int?>, // null = string muted, Int = fret number
    val base: Int,
    val barre: Barre?,
    val roots: List<Boolean>,
    val cost: Double,
    val pos: Int
)

object ChordSolver {

    private fun evaluate(
        combo: List<Int?>,
        openSemis: List<Int>,
        rootPc: Int,
        reqPcs: Set<Int>,
        minSounded: Int
    ): Voicing? {
        val n = combo.size
        val sounded = combo.mapIndexedNotNull { index, fret ->
            if (fret != null) index to fret else null
        }
        if (sounded.size < minSounded) return null

        val soundedIndices = sounded.map { it.first }
        // Non ammettiamo corde mute tra la prima e l'ultima corda suonata
        if (soundedIndices.last() - soundedIndices.first() + 1 != soundedIndices.size) {
            return null
        }

        val pcs = sounded.map { (i, f) ->
            Math.floorMod(69 + openSemis[i] + f, 12)
        }.toSet()

        if (!pcs.containsAll(reqPcs)) return null

        val fretted = sounded.filter { it.second > 0 }
        var barre: Barre? = null
        val fmax: Int
        val fingers: Int
        val base: Int
        val span: Int

        if (fretted.isNotEmpty()) {
            val fmin = fretted.minOf { it.second }
            fmax = fretted.maxOf { it.second }
            if (fmax - fmin > 3) return null

            val atMin = fretted.filter { it.second == fmin }.map { it.first }
            if (atMin.size >= 2) {
                val lo = atMin.minOrNull()!!
                val hi = atMin.maxOrNull()!!
                val allAtLeastMin = (lo..hi).all { j ->
                    val f = combo[j]
                    f != null && f >= fmin
                }
                if (allAtLeastMin) {
                    barre = Barre(fmin, lo, hi)
                }
            }

            fingers = if (barre != null) fretted.size - atMin.size + 1 else fretted.size
            if (fingers > 4) return null

            base = if (fmax <= 4) 1 else fmin
            span = fmax - fmin
        } else {
            fmax = 0
            fingers = 0
            base = 1
            span = 0
        }

        val opens = sounded.count { it.second == 0 }
        val muted = n - sounded.size
        val bassIndex = soundedIndices.first()
        val bassPc = Math.floorMod(69 + openSemis[bassIndex] + (combo[bassIndex] ?: 0), 12)

        val bassCost = if (bassPc == rootPc) 0.0 else 4.5
        val cost = 0.8 * fmax + 0.4 * span + 2.0 * muted + 0.9 * fingers - 0.7 * opens + bassCost

        val roots = combo.mapIndexed { i, f ->
            f != null && Math.floorMod(69 + openSemis[i] + f, 12) == rootPc
        }

        val pos = if (fretted.isNotEmpty()) fretted.minOf { it.second } else 0

        return Voicing(
            frets = combo,
            base = base,
            barre = barre,
            roots = roots,
            cost = cost,
            pos = pos
        )
    }

    fun findVoicings(
        openSemis: List<Int>,
        rootPc: Int,
        intervals: List<Int>,
        required: List<Int>,
        limit: Int = 3
    ): List<Voicing> {
        val n = openSemis.size
        if (n == 0) return emptyList()

        val tones = intervals.map { Math.floorMod(rootPc + it, 12) }.toSet()
        val reqPcs = required.map { Math.floorMod(rootPc + it, 12) }.toSet()
        val minSounded = if (n <= 4) 3 else 4

        val seen = mutableMapOf<List<Int?>, Voicing?>()

        for (base in 1..9) {
            val fretsOk = if (base <= 3) {
                listOf(0) + (base..base + 3).toList()
            } else {
                (base..base + 3).toList()
            }

            val opts = openSemis.map { s ->
                val matchingFrets = fretsOk.filter { f ->
                    Math.floorMod(69 + s + f, 12) in tones
                }
                listOf<Int?>(null) + matchingFrets
            }

            cartesianProduct(opts) { combo ->
                if (!seen.containsKey(combo)) {
                    val evaluated = evaluate(combo, openSemis, rootPc, reqPcs, minSounded)
                    seen[combo] = evaluated
                }
            }
        }

        val cands = seen.values.filterNotNull().sortedBy { it.cost }
        val chosen = mutableListOf<Voicing>()

        // 1. Posizioni distanti
        for (v in cands) {
            if (chosen.all { abs(v.pos - it.pos) >= 3 }) {
                chosen.add(v)
            }
            if (chosen.size == limit) break
        }

        // 2. Altre diteggiature con forme distinte
        for (v in cands) {
            if (chosen.size == limit) break
            if (v !in chosen) {
                val differsEnough = chosen.all { c ->
                    v.frets.zip(c.frets).count { (a, b) -> a != b } > 1
                }
                if (differsEnough) {
                    chosen.add(v)
                }
            }
        }

        return chosen.sortedBy { it.cost }
    }

    private fun cartesianProduct(
        lists: List<List<Int?>>,
        action: (List<Int?>) -> Unit
    ) {
        fun helper(depth: Int, current: Array<Int?>) {
            if (depth == lists.size) {
                action(current.toList())
                return
            }
            for (item in lists[depth]) {
                current[depth] = item
                helper(depth + 1, current)
            }
        }
        helper(0, Array(lists.size) { null })
    }
}
