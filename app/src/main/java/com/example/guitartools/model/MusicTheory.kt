package com.example.guitartools.model

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

data class NoteInfo(
    val name: String,
    val octave: Int,
    val cents: Double,
    val nearestSemis: Int
)

data class ChordTypeDef(
    val intervals: List<Int>,
    val required: List<Int>,
    val suffix: String
)

data class KeyInfo(
    val title: String,
    val sig: String,
    val chords: String,
    val relative: String,
    val neighbors: String
)

data class ProgressionItem(
    val name: String,
    val chords: List<ProgressionChordDef>
)

data class ProgressionChordDef(
    val offset: Int,
    val typeKey: String,
    val roman: String
)

data class CalculatedProgressionChord(
    val pc: Int,
    val typeKey: String,
    val name: String,
    val roman: String
)

object MusicTheory {

    val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val FLAT_NAMES = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    val MAJ_LABELS = listOf("C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
    val MIN_LABELS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "Bb", "B")

    val SOLFEGE = mapOf(
        "C" to "Do", "D" to "Re", "E" to "Mi", "F" to "Fa",
        "G" to "Sol", "A" to "La", "B" to "Si"
    )

    val LABEL_PC = mapOf(
        "C" to 0, "C#" to 1, "Db" to 1, "D" to 2, "D#" to 3, "Eb" to 3, "E" to 4,
        "F" to 5, "F#" to 6, "G" to 7, "G#" to 8, "Ab" to 8, "A" to 9, "Bb" to 10, "B" to 11
    )

    val TUNINGS = mapOf(
        "Standard (EADGBE)" to listOf("E2", "A2", "D3", "G3", "B3", "E4"),
        "Mezzo tono sotto (Eb)" to listOf("D#2", "G#2", "C#3", "F#3", "A#3", "D#4"),
        "Un tono sotto (D)" to listOf("D2", "G2", "C3", "F3", "A3", "D4"),
        "Drop D" to listOf("D2", "A2", "D3", "G3", "B3", "E4"),
        "Drop C" to listOf("C2", "G2", "C3", "F3", "A3", "D4"),
        "DADGAD" to listOf("D2", "A2", "D3", "G3", "A3", "D4"),
        "Open G (DGDGBD)" to listOf("D2", "G2", "D3", "G3", "B3", "D4"),
        "Open D (DADF#AD)" to listOf("D2", "A2", "D3", "F#3", "A3", "D4"),
        "Open E (EBEG#BE)" to listOf("E2", "B2", "E3", "G#3", "B3", "E4"),
        "Basso 4 corde" to listOf("E1", "A1", "D2", "G2"),
        "Ukulele" to listOf("G4", "C4", "E4", "A4"),
        "Cromatico" to emptyList()
    )

    fun noteToSemis(name: String): Int {
        val notePart = name.dropLast(1)
        val octavePart = name.takeLast(1).toInt()
        val idx = NOTE_NAMES.indexOf(notePart)
        return (12 * (octavePart + 1) + idx) - 69
    }

    fun semisToFreq(semis: Int, a4: Double): Double {
        return a4 * 2.0.pow(semis / 12.0)
    }

    fun freqToNote(freq: Double, a4: Double): NoteInfo {
        val semis = 12.0 * log2(freq / a4)
        val nearest = semis.roundToInt()
        val cents = (semis - nearest) * 100.0
        val idx = Math.floorMod(nearest + 9, 12)
        val octave = 4 + Math.floorDiv(nearest + 9, 12)
        return NoteInfo(NOTE_NAMES[idx], octave, cents, nearest)
    }

    val CHORD_TYPES = mapOf(
        "maggiore" to ChordTypeDef(listOf(0, 4, 7), listOf(0, 4), ""),
        "minore" to ChordTypeDef(listOf(0, 3, 7), listOf(0, 3), "m"),
        "7" to ChordTypeDef(listOf(0, 4, 7, 10), listOf(0, 4, 10), "7"),
        "maj7" to ChordTypeDef(listOf(0, 4, 7, 11), listOf(0, 4, 11), "maj7"),
        "m7" to ChordTypeDef(listOf(0, 3, 7, 10), listOf(0, 3, 10), "m7"),
        "sus2" to ChordTypeDef(listOf(0, 2, 7), listOf(0, 2), "sus2"),
        "sus4" to ChordTypeDef(listOf(0, 5, 7), listOf(0, 5), "sus4"),
        "dim" to ChordTypeDef(listOf(0, 3, 6), listOf(0, 3, 6), "dim"),
        "m7b5" to ChordTypeDef(listOf(0, 3, 6, 10), listOf(0, 3, 6, 10), "m7b5")
    )

    val SCALES = mapOf(
        "Maggiore" to listOf(0, 2, 4, 5, 7, 9, 11),
        "Minore naturale" to listOf(0, 2, 3, 5, 7, 8, 10),
        "Minore armonica" to listOf(0, 2, 3, 5, 7, 8, 11),
        "Minore melodica" to listOf(0, 2, 3, 5, 7, 9, 11),
        "Pentatonica maggiore" to listOf(0, 2, 4, 7, 9),
        "Pentatonica minore" to listOf(0, 3, 5, 7, 10),
        "Blues minore" to listOf(0, 3, 5, 6, 7, 10),
        "Blues maggiore" to listOf(0, 2, 3, 4, 7, 9),
        "Dorica" to listOf(0, 2, 3, 5, 7, 9, 10),
        "Frigia" to listOf(0, 1, 3, 5, 7, 8, 10),
        "Lidia" to listOf(0, 2, 4, 6, 7, 9, 11),
        "Misolidia" to listOf(0, 2, 4, 5, 7, 9, 10),
        "Locria" to listOf(0, 1, 3, 5, 6, 8, 10),
        "Bebop dominante" to listOf(0, 2, 4, 5, 7, 9, 10, 11),
        "Bebop maggiore" to listOf(0, 2, 4, 5, 7, 8, 9, 11),
        "Bebop dorica" to listOf(0, 2, 3, 4, 5, 7, 9, 10),
        "Lidia dominante" to listOf(0, 2, 4, 6, 7, 9, 10),
        "Misolidia b6" to listOf(0, 2, 4, 5, 7, 8, 10),
        "Locria #2" to listOf(0, 2, 3, 5, 6, 8, 10),
        "Alterata" to listOf(0, 1, 3, 4, 6, 8, 10),
        "Tono intero" to listOf(0, 2, 4, 6, 8, 10),
        "Diminuita T-S" to listOf(0, 2, 3, 5, 6, 8, 9, 11),
        "Diminuita S-T" to listOf(0, 1, 3, 4, 6, 7, 9, 10)
    )

    val SCALE_SPECIAL = mapOf(
        "Blues minore" to listOf(6),
        "Blues maggiore" to listOf(3),
        "Bebop dominante" to listOf(11),
        "Bebop maggiore" to listOf(8),
        "Bebop dorica" to listOf(4)
    )

    val STEP_NAMES = mapOf(1 to "S", 2 to "T", 3 to "T+S")

    val CIRCLE_MAJOR = listOf("C", "G", "D", "A", "E", "B", "F#", "Db", "Ab", "Eb", "Bb", "F")
    val CIRCLE_MINOR = listOf("Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "Bbm", "Fm", "Cm", "Gm", "Dm")
    private val ROMAN_MAJOR = listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")
    private val ROMAN_MINOR = listOf("i", "ii°", "III", "iv", "v", "VI", "VII")

    fun keyInfo(i: Int, minor: Boolean): KeyInfo {
        val names = if (i >= 7) FLAT_NAMES else NOTE_NAMES
        val tonic = Math.floorMod(7 * i + (if (minor) 9 else 0), 12)
        val ints = if (minor) listOf(0, 2, 3, 5, 7, 8, 10) else listOf(0, 2, 4, 5, 7, 9, 11)
        val qual = if (minor) listOf("m", "dim", "", "m", "m", "", "") else listOf("", "m", "m", "", "", "m", "dim")
        val roman = if (minor) ROMAN_MINOR else ROMAN_MAJOR
        val chordsList = roman.indices.map { idx ->
            "${roman[idx]} ${names[(tonic + ints[idx]) % 12]}${qual[idx]}"
        }
        val sig = when {
            i == 0 -> "nessuna alterazione"
            i <= 6 -> "$i diesis"
            else -> {
                val n = 12 - i
                if (n == 1) "1 bemolle" else "$n bemolli"
            }
        }
        val ring = if (minor) CIRCLE_MINOR else CIRCLE_MAJOR
        val other = if (minor) CIRCLE_MAJOR else CIRCLE_MINOR
        val title = if (minor) CIRCLE_MINOR[i].dropLast(1) + " minore" else CIRCLE_MAJOR[i] + " maggiore"

        return KeyInfo(
            title = title,
            sig = "Armatura: $sig",
            chords = chordsList.joinToString("  ·  "),
            relative = (if (minor) "Relativa maggiore: " else "Relativa minore: ") + other[i],
            neighbors = "Quinta sopra: ${ring[(i + 1) % 12]}   ·   Quinta sotto: ${ring[(i + 11) % 12]}"
        )
    }

    fun solfege(label: String): String {
        if (label.isEmpty()) return label
        val first = label.substring(0, 1)
        val sol = SOLFEGE[first] ?: first
        return sol + label.substring(1)
    }

    fun rootLabel(pc: Int, minor: Boolean): String {
        val list = if (minor) MIN_LABELS else MAJ_LABELS
        return list[Math.floorMod(pc, 12)]
    }

    fun keyName(pc: Int, minor: Boolean): String {
        val lab = rootLabel(pc, minor)
        val mode = if (minor) "minore" else "maggiore"
        return "$lab $mode (${solfege(lab)})"
    }

    fun noteNameWithOctave(midi: Int): String {
        val pc = Math.floorMod(midi, 12)
        val octave = (midi / 12) - 1
        return "${NOTE_NAMES[pc]}$octave"
    }

    fun spellNames(tonic: Int, minor: Boolean): List<String> {
        val rel = if (minor) (tonic + 3) % 12 else tonic
        return if ((7 * rel) % 12 >= 7) FLAT_NAMES else NOTE_NAMES
    }

    fun progressionChords(tonic: Int, minor: Boolean, prog: List<ProgressionChordDef>): List<CalculatedProgressionChord> {
        val names = spellNames(tonic, minor)
        return prog.map { def ->
            val pc = (tonic + def.offset) % 12
            val typeDef = CHORD_TYPES[def.typeKey]
            val chordName = names[pc] + (typeDef?.suffix ?: "")
            CalculatedProgressionChord(pc, def.typeKey, chordName, def.roman)
        }
    }

    private const val M = "maggiore"
    private const val m = "minore"

    val PROG_MAJOR = listOf(
        ProgressionItem("Giro di {k} (I-vi-IV-V)", listOf(ProgressionChordDef(0, M, "I"), ProgressionChordDef(9, m, "vi"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(7, M, "V"))),
        ProgressionItem("Pop (I-V-vi-IV)", listOf(ProgressionChordDef(0, M, "I"), ProgressionChordDef(7, M, "V"), ProgressionChordDef(9, m, "vi"), ProgressionChordDef(5, M, "IV"))),
        ProgressionItem("Anni '50 (vi-IV-I-V)", listOf(ProgressionChordDef(9, m, "vi"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(0, M, "I"), ProgressionChordDef(7, M, "V"))),
        ProgressionItem("Cadenza (I-IV-V-I)", listOf(ProgressionChordDef(0, M, "I"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(7, M, "V"), ProgressionChordDef(0, M, "I"))),
        ProgressionItem("Rock (I-bVII-IV-I)", listOf(ProgressionChordDef(0, M, "I"), ProgressionChordDef(10, M, "bVII"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(0, M, "I"))),
        ProgressionItem("Canone di Pachelbel", listOf(ProgressionChordDef(0, M, "I"), ProgressionChordDef(7, M, "V"), ProgressionChordDef(9, m, "vi"), ProgressionChordDef(4, m, "iii"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(0, M, "I"), ProgressionChordDef(5, M, "IV"), ProgressionChordDef(7, M, "V"))),
        ProgressionItem("Jazz ii-V-I", listOf(ProgressionChordDef(2, "m7", "ii7"), ProgressionChordDef(7, "7", "V7"), ProgressionChordDef(0, "maj7", "Imaj7"))),
        ProgressionItem("Jazz I-vi-ii-V", listOf(ProgressionChordDef(0, "maj7", "Imaj7"), ProgressionChordDef(9, "m7", "vi7"), ProgressionChordDef(2, "m7", "ii7"), ProgressionChordDef(7, "7", "V7"))),
        ProgressionItem("Blues 12 battute", listOf(
            ProgressionChordDef(0, "7", "I7"), ProgressionChordDef(0, "7", "I7"), ProgressionChordDef(0, "7", "I7"), ProgressionChordDef(0, "7", "I7"),
            ProgressionChordDef(5, "7", "IV7"), ProgressionChordDef(5, "7", "IV7"), ProgressionChordDef(0, "7", "I7"), ProgressionChordDef(0, "7", "I7"),
            ProgressionChordDef(7, "7", "V7"), ProgressionChordDef(5, "7", "IV7"), ProgressionChordDef(0, "7", "I7"), ProgressionChordDef(7, "7", "V7")
        ))
    )

    val PROG_MINOR = listOf(
        ProgressionItem("Andalusa (i-VII-VI-V)", listOf(ProgressionChordDef(0, m, "i"), ProgressionChordDef(10, M, "VII"), ProgressionChordDef(8, M, "VI"), ProgressionChordDef(7, M, "V"))),
        ProgressionItem("Minore (i-VI-III-VII)", listOf(ProgressionChordDef(0, m, "i"), ProgressionChordDef(8, M, "VI"), ProgressionChordDef(3, M, "III"), ProgressionChordDef(10, M, "VII"))),
        ProgressionItem("Minore (i-iv-V-i)", listOf(ProgressionChordDef(0, m, "i"), ProgressionChordDef(5, m, "iv"), ProgressionChordDef(7, M, "V"), ProgressionChordDef(0, m, "i"))),
        ProgressionItem("Jazz ii-V-i", listOf(ProgressionChordDef(2, "m7b5", "iiø7"), ProgressionChordDef(7, "7", "V7"), ProgressionChordDef(0, "m7", "i7"))),
        ProgressionItem("Blues minore 12 battute", listOf(
            ProgressionChordDef(0, "m7", "i7"), ProgressionChordDef(0, "m7", "i7"), ProgressionChordDef(0, "m7", "i7"), ProgressionChordDef(0, "m7", "i7"),
            ProgressionChordDef(5, "m7", "iv7"), ProgressionChordDef(5, "m7", "iv7"), ProgressionChordDef(0, "m7", "i7"), ProgressionChordDef(0, "m7", "i7"),
            ProgressionChordDef(8, "7", "bVI7"), ProgressionChordDef(7, "7", "V7"), ProgressionChordDef(0, "m7", "i7"), ProgressionChordDef(7, "7", "V7")
        ))
    )
}
