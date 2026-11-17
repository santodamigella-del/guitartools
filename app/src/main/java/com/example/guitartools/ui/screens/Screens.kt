package com.example.guitartools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitartools.audio.AudioSynthesizer
import com.example.guitartools.audio.MicInput
import com.example.guitartools.audio.PitchDetector
import com.example.guitartools.model.ChordSolver
import com.example.guitartools.model.KeyEstimator
import com.example.guitartools.model.MusicTheory
import com.example.guitartools.model.Voicing
import com.example.guitartools.ui.components.AppAccent
import com.example.guitartools.ui.components.AppGood
import com.example.guitartools.ui.components.AppMuted
import com.example.guitartools.ui.components.AppSurface
import com.example.guitartools.ui.components.AppSurface2
import com.example.guitartools.ui.components.AppText
import com.example.guitartools.ui.components.AppWarn
import com.example.guitartools.ui.components.BeatDots
import com.example.guitartools.ui.components.ChordDiagram
import com.example.guitartools.ui.components.CircleOfFifths
import com.example.guitartools.ui.components.Fretboard
import com.example.guitartools.ui.components.MeterGauge
import com.example.guitartools.ui.components.PitchBars
import kotlinx.coroutines.delay
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

@Composable
fun StyledSpinner(
    selectedText: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            color = AppSurface2,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = selectedText,
                    color = AppText,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppSurface2)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = AppText, fontSize = 14.sp) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChipButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) AppAccent else AppSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = AppText,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PillButton(
    text: String,
    isHot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isHot) AppAccent else AppSurface2,
            contentColor = AppText
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- TUNER VIEW
@Composable
fun TunerView(
    mic: MicInput,
    audioSynth: AudioSynthesizer,
    currentTuningName: String,
    tuningStrings: List<Pair<String, Int>>,
    a4: Double,
    onA4Change: (Double) -> Unit,
    fmin: Float,
    fmax: Float,
    winLen: Int,
    modifier: Modifier = Modifier
) {
    var targetString by remember { mutableStateOf("Auto") }
    var centsSmooth by remember { mutableFloatStateOf(0f) }
    var meterActive by remember { mutableStateOf(false) }
    var inTune by remember { mutableStateOf(false) }
    var noteLabel by remember { mutableStateOf("--") }
    var noteColor by remember { mutableStateOf(AppText) }
    var freqText by remember { mutableStateOf("Suona una corda") }
    var hintText by remember { mutableStateOf("") }
    var isPlayingTone by remember { mutableStateOf(false) }

    val history = remember { ArrayDeque<Float>(7) }
    var outliers by remember { mutableIntStateOf(0) }
    var lastValidTime by remember { mutableDoubleStateOf(0.0) }
    var lastLabel by remember { mutableStateOf<String?>(null) }

    val targetMap = remember(tuningStrings) { tuningStrings.toMap() }

    // Loop analisi microfono
    LaunchedEffect(currentTuningName, a4, targetString, isPlayingTone) {
        history.clear()
        outliers = 0
        meterActive = false

        while (true) {
            delay(80)
            if (isPlayingTone) continue

            val x = mic.snapshot(winLen)
            if (x == null) continue

            val freq = PitchDetector.detectPitch(x, mic.sampleRate, fmin, fmax)
            val now = System.currentTimeMillis() / 1000.0

            if (freq == null) {
                if (now - lastValidTime > 0.8) {
                    history.clear()
                    outliers = 0
                    meterActive = false
                }
                continue
            }

            // Scarta outlier
            if (history.size >= 3) {
                val sorted = history.sorted()
                val med = sorted[sorted.size / 2]
                val diffCents = abs(1200 * log2(freq / med))
                if (diffCents > 100) {
                    outliers++
                    if (outliers >= 3) {
                        history.clear()
                        outliers = 0
                    } else {
                        continue
                    }
                } else {
                    outliers = 0
                }
            }

            if (history.size >= 7) history.removeFirst()
            history.addLast(freq)
            lastValidTime = now

            val sortedHist = history.sorted()
            val filteredFreq = sortedHist[sortedHist.size / 2].toDouble()

            val targetF: Double
            val cents: Float
            val label: String

            if (targetString != "Auto" && targetMap.containsKey(targetString)) {
                val semis = targetMap[targetString]!!
                targetF = MusicTheory.semisToFreq(semis, a4)
                cents = (1200 * log2(filteredFreq / targetF)).toFloat()
                label = targetString
            } else if (tuningStrings.isNotEmpty()) {
                val best = tuningStrings.minByOrNull { (_, semis) ->
                    abs(log2(filteredFreq / MusicTheory.semisToFreq(semis, a4)))
                }!!
                targetF = MusicTheory.semisToFreq(best.second, a4)
                cents = (1200 * log2(filteredFreq / targetF)).toFloat()
                label = best.first
            } else {
                val noteInfo = MusicTheory.freqToNote(filteredFreq, a4)
                targetF = MusicTheory.semisToFreq(noteInfo.nearestSemis, a4)
                cents = noteInfo.cents.toFloat()
                label = "${noteInfo.name}${noteInfo.octave}"
            }

            if (label != lastLabel || !meterActive) {
                centsSmooth = cents
            } else {
                centsSmooth = 0.5f * centsSmooth + 0.5f * cents
            }
            lastLabel = label

            val shownCents = centsSmooth
            val isInTune = abs(shownCents) < 5f

            noteLabel = label
            inTune = isInTune
            noteColor = if (isInTune) AppGood else AppText
            freqText = String.format("%.1f Hz   (target %.1f Hz)", filteredFreq, targetF)
            hintText = if (isInTune) "Intonata!" else String.format("%+.0f cent  ", shownCents) + (if (shownCents < 0) "-> tira su" else "-> allenta")
            meterActive = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Note Label Display
        Text(
            text = noteLabel,
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = noteColor
        )

        Text(
            text = freqText,
            fontSize = 15.sp,
            color = AppMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Gauge Meter Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            MeterGauge(
                cents = centsSmooth,
                active = meterActive,
                inTune = inTune,
                modifier = Modifier.padding(8.dp)
            )
        }

        Text(
            text = hintText,
            fontSize = 18.sp,
            color = AppText,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // String selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ChipButton(
                text = "Auto",
                isSelected = targetString == "Auto",
                onClick = {
                    targetString = "Auto"
                    audioSynth.stopTone()
                    isPlayingTone = false
                }
            )
            tuningStrings.forEach { (name, _) ->
                ChipButton(
                    text = name,
                    isSelected = targetString == name,
                    onClick = {
                        targetString = name
                        if (isPlayingTone) {
                            val semis = targetMap[name]
                            if (semis != null) {
                                audioSynth.playTone(MusicTheory.semisToFreq(semis, a4))
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // A4 adjustment and Play Tone row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PillButton(
                text = "-",
                onClick = { onA4Change((a4 - 1.0).coerceIn(415.0, 466.0)) },
                modifier = Modifier.width(54.dp)
            )

            Text(
                text = String.format("A4 = %.0f Hz", a4),
                color = AppMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            PillButton(
                text = "+",
                onClick = { onA4Change((a4 + 1.0).coerceIn(415.0, 466.0)) },
                modifier = Modifier.width(54.dp)
            )

            PillButton(
                text = if (isPlayingTone) "Stop" else "Suona",
                isHot = isPlayingTone,
                onClick = {
                    if (isPlayingTone) {
                        audioSynth.stopTone()
                        isPlayingTone = false
                        hintText = ""
                    } else {
                        if (targetString == "Auto" || !targetMap.containsKey(targetString)) {
                            hintText = "Scegli una corda per sentire il tono"
                        } else {
                            val semis = targetMap[targetString]!!
                            audioSynth.playTone(MusicTheory.semisToFreq(semis, a4))
                            isPlayingTone = true
                            noteLabel = targetString
                            hintText = "Accorda la corda a orecchio"
                            meterActive = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- VOICE VIEW
@Composable
fun VoiceView(
    mic: MicInput,
    a4: Double,
    onOpenGiri: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isListening by remember { mutableStateOf(false) }
    var liveNoteText by remember { mutableStateOf("--") }
    var liveFreqText by remember { mutableStateOf("Premi Ascolta e canta qualche frase") }
    var keyVerdictText by remember { mutableStateOf("") }
    var altHypothesesText by remember { mutableStateOf("") }
    var rangeText by remember { mutableStateOf("") }

    val hist = remember { IntArray(12) }
    val notes = remember { mutableListOf<Int>() }
    val recentPitches = remember { ArrayDeque<Double>(3) }

    var keyPc by remember { mutableIntStateOf(-1) }
    var keyMinor by remember { mutableStateOf(false) }

    LaunchedEffect(isListening) {
        if (!isListening) return@LaunchedEffect

        while (isListening) {
            delay(100)
            val x = mic.snapshot(4096) ?: continue
            val f = PitchDetector.detectPitch(x, mic.sampleRate, 70f, 1000f)

            if (f == null) {
                recentPitches.clear()
                liveNoteText = "--"
                continue
            }

            val midiVal = 69.0 + 12.0 * log2(f.toDouble() / a4)
            if (recentPitches.size >= 3) recentPitches.removeFirst()
            recentPitches.addLast(midiVal)

            if (recentPitches.size < 3) continue
            val maxDiff = recentPitches.maxOrNull()!! - recentPitches.minOrNull()!!
            if (maxDiff > 1.2) continue // pitch non ancora stabile

            val sortedPitches = recentPitches.sorted()
            val medianMidi = sortedPitches[1]
            val noteInt = medianMidi.roundToInt()

            hist[Math.floorMod(noteInt, 12)]++
            notes.add(noteInt)

            liveNoteText = MusicTheory.noteNameWithOctave(noteInt)
            liveFreqText = String.format("%.0f Hz    %+.0f cent", f, (medianMidi - noteInt) * 100)

            // Update range & key verdict
            val count = notes.size
            if (count >= 10) {
                val sortedNotes = notes.sorted()
                val loIndex = (count * 0.05).toInt().coerceIn(0, count - 1)
                val hiIndex = (count * 0.95).toInt().coerceIn(0, count - 1)
                rangeText = "Estensione: ${MusicTheory.noteNameWithOctave(sortedNotes[loIndex])} - ${MusicTheory.noteNameWithOctave(sortedNotes[hiIndex])}"
            }

            if (count < 15) {
                keyVerdictText = "Sto ascoltando... ($count/15)"
            } else {
                val ranked = KeyEstimator.estimateKeys(hist)
                if (ranked.isNotEmpty()) {
                    val top = ranked.first()
                    keyPc = top.pc
                    keyMinor = top.isMinor
                    keyVerdictText = "Tonalità: " + MusicTheory.keyName(top.pc, top.isMinor)

                    val alts = ranked.drop(1).take(3).joinToString("  ·  ") {
                        MusicTheory.keyName(it.pc, it.isMinor)
                    }
                    altHypothesesText = "Altre ipotesi:  $alts"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = liveNoteText,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = AppText
        )

        Text(
            text = liveFreqText,
            fontSize = 15.sp,
            color = AppMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pitch Bars Canvas Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 4.dp)
        ) {
            PitchBars(
                hist = hist,
                keyPc = keyPc,
                keyMinor = keyMinor,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = keyVerdictText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppText
        )

        Text(
            text = altHypothesesText,
            fontSize = 13.sp,
            color = AppMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        Text(
            text = rangeText,
            fontSize = 14.sp,
            color = AppText,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(
                text = if (isListening) "Stop" else "Ascolta",
                isHot = isListening,
                onClick = {
                    if (isListening) {
                        isListening = false
                        liveNoteText = "--"
                    } else {
                        for (i in 0 until 12) hist[i] = 0
                        notes.clear()
                        recentPitches.clear()
                        keyPc = -1
                        keyVerdictText = "Sto ascoltando... (0/15)"
                        altHypothesesText = ""
                        rangeText = ""
                        liveFreqText = "Canta una melodia, anche a mezza voce"
                        isListening = true
                    }
                },
                modifier = Modifier.weight(1f)
            )

            PillButton(
                text = "Giri armonici",
                onClick = {
                    if (keyPc >= 0) {
                        isListening = false
                        onOpenGiri(keyPc, keyMinor)
                    } else {
                        liveFreqText = "Prima canta qualche frase, poi apro i giri"
                    }
                },
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

// ---------------------------------------------------------------- CHORD VIEW
@Composable
fun ChordView(
    tuningStrings: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    var rootText by remember { mutableStateOf("C") }
    var typeText by remember { mutableStateOf("maggiore") }

    val chordTypeDef = MusicTheory.CHORD_TYPES[typeText] ?: MusicTheory.CHORD_TYPES["maggiore"]!!
    val rootPc = MusicTheory.NOTE_NAMES.indexOf(rootText).coerceAtLeast(0)

    val chordTitle = rootText + chordTypeDef.suffix
    val notesText = "Note:  " + chordTypeDef.intervals.joinToString("   ") {
        MusicTheory.NOTE_NAMES[(rootPc + it) % 12]
    }

    val openSemis = remember(tuningStrings) { tuningStrings.map { it.second } }
    val voicings = remember(openSemis, rootPc, typeText) {
        ChordSolver.findVoicings(openSemis, rootPc, chordTypeDef.intervals, chordTypeDef.required)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyledSpinner(
                selectedText = rootText,
                options = MusicTheory.NOTE_NAMES,
                onOptionSelected = { rootText = it },
                modifier = Modifier.weight(0.35f)
            )
            StyledSpinner(
                selectedText = typeText,
                options = MusicTheory.CHORD_TYPES.keys.toList(),
                onOptionSelected = { typeText = it },
                modifier = Modifier.weight(0.65f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = chordTitle,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = AppText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = notesText,
            fontSize = 15.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (voicings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessuna posizione comoda per questa accordatura", color = AppMuted)
                }
            } else {
                voicings.forEach { v ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ChordDiagram(
                                voicing = v,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = v.frets.joinToString(" ") { if (it == null) "x" else it.toString() },
                                fontSize = 13.sp,
                                color = AppMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (tuningStrings.isNotEmpty()) "Posizioni per accordatura selezionata" else "Cromatico: posizioni in accordatura standard",
            fontSize = 13.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ---------------------------------------------------------------- GIRI VIEW
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GiriView(
    tuningStrings: List<Pair<String, Int>>,
    initialPc: Int = 0,
    initialMinor: Boolean = false,
    modifier: Modifier = Modifier
) {
    var rootText by remember { mutableStateOf(MusicTheory.rootLabel(initialPc, initialMinor)) }
    var modeText by remember { mutableStateOf(if (initialMinor) "minore" else "maggiore") }

    val isMinor = (modeText == "minore")
    val progs = if (isMinor) MusicTheory.PROG_MINOR else MusicTheory.PROG_MAJOR
    val rootSolfege = MusicTheory.solfege(rootText)
    val progNames = progs.map { it.name.replace("{k}", rootSolfege) }

    var selectedProgIndex by remember { mutableIntStateOf(0) }
    if (selectedProgIndex >= progNames.size) selectedProgIndex = 0

    val currentProg = progs[selectedProgIndex]
    val tonic = MusicTheory.LABEL_PC[rootText] ?: 0
    val calculatedChords = MusicTheory.progressionChords(tonic, isMinor, currentProg.chords)

    var selectedChordIndex by remember { mutableIntStateOf(0) }
    if (selectedChordIndex >= calculatedChords.size) selectedChordIndex = 0

    val currentChord = calculatedChords.getOrNull(selectedChordIndex) ?: calculatedChords[0]
    val openSemis = remember(tuningStrings) { tuningStrings.map { it.second } }

    val currentVoicings = remember(openSemis, currentChord) {
        val typeDef = MusicTheory.CHORD_TYPES[currentChord.typeKey] ?: MusicTheory.CHORD_TYPES["maggiore"]!!
        ChordSolver.findVoicings(openSemis, currentChord.pc, typeDef.intervals, typeDef.required)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyledSpinner(
                selectedText = rootText,
                options = MusicTheory.MAJ_LABELS,
                onOptionSelected = { rootText = it },
                modifier = Modifier.weight(0.35f)
            )
            StyledSpinner(
                selectedText = modeText,
                options = listOf("maggiore", "minore"),
                onOptionSelected = {
                    modeText = it
                    selectedProgIndex = 0
                },
                modifier = Modifier.weight(0.65f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        StyledSpinner(
            selectedText = progNames[selectedProgIndex],
            options = progNames,
            onOptionSelected = { selectedProgIndex = progNames.indexOf(it).coerceAtLeast(0) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Grid of progression chord chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 4
        ) {
            calculatedChords.forEachIndexed { idx, chord ->
                ChipButton(
                    text = "${chord.name}\n${chord.roman}",
                    isSelected = idx == selectedChordIndex,
                    onClick = { selectedChordIndex = idx },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Voicings display for selected chord
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (currentVoicings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessuna posizione comoda", color = AppMuted)
                }
            } else {
                currentVoicings.forEach { v ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ChordDiagram(voicing = v, modifier = Modifier.weight(1f))
                            Text(
                                text = v.frets.joinToString(" ") { if (it == null) "x" else it.toString() },
                                fontSize = 13.sp,
                                color = AppMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val noteNames = MusicTheory.spellNames(tonic, isMinor)
        val chordTypeDef = MusicTheory.CHORD_TYPES[currentChord.typeKey]
        val noteBreakdown = chordTypeDef?.intervals?.joinToString("  ") {
            noteNames[(currentChord.pc + it) % 12]
        } ?: ""

        Text(
            text = "${currentChord.name}   ·   note: $noteBreakdown",
            fontSize = 14.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ---------------------------------------------------------------- SCALE VIEW
@Composable
fun ScaleView(
    tuningStrings: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    var rootText by remember { mutableStateOf("A") }
    var scaleText by remember { mutableStateOf("Pentatonica minore") }

    val ints = MusicTheory.SCALES[scaleText] ?: MusicTheory.SCALES["Pentatonica minore"]!!
    val rootPc = MusicTheory.NOTE_NAMES.indexOf(rootText).coerceAtLeast(0)
    val pcs = ints.map { (rootPc + it) % 12 }.toSet()

    val notesDisplay = pcs.joinToString("  ") { MusicTheory.NOTE_NAMES[it] }
    val diffs = (0 until ints.size - 1).map { ints[it + 1] - ints[it] } + (12 - ints.last())
    val stepsDisplay = "Passi:  " + diffs.joinToString(" ") { MusicTheory.STEP_NAMES[it] ?: it.toString() }

    val specialSet = (MusicTheory.SCALE_SPECIAL[scaleText] ?: emptyList()).map { (rootPc + it) % 12 }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StyledSpinner(
                selectedText = rootText,
                options = MusicTheory.NOTE_NAMES,
                onOptionSelected = { rootText = it },
                modifier = Modifier.weight(0.3f)
            )
            StyledSpinner(
                selectedText = scaleText,
                options = MusicTheory.SCALES.keys.toList(),
                onOptionSelected = { scaleText = it },
                modifier = Modifier.weight(0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = notesDisplay,
            fontSize = if (pcs.size <= 6) 26.sp else if (pcs.size == 7) 22.sp else 19.sp,
            fontWeight = FontWeight.Bold,
            color = AppText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = stepsDisplay,
            fontSize = 14.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            Fretboard(
                strings = tuningStrings,
                pcs = pcs,
                rootPc = rootPc,
                specialPcs = specialSet,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val legendText = "Ambra = tonica   Blu = note della scala" +
                (if (specialSet.isNotEmpty()) "   Verde = nota blue / passaggio" else "") +
                "\nI nomi usano i diesis (A# = Sib, D# = Mib...)"

        Text(
            text = legendText,
            fontSize = 13.sp,
            color = AppMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ---------------------------------------------------------------- CIRCLE VIEW
@Composable
fun CircleView(
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedMinor by remember { mutableStateOf(false) }

    val info = MusicTheory.keyInfo(selectedIndex, selectedMinor)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            CircleOfFifths(
                selectedIndex = selectedIndex,
                selectedMinor = selectedMinor,
                onSelectKey = { idx, minor ->
                    selectedIndex = idx
                    selectedMinor = minor
                },
                modifier = Modifier.padding(8.dp)
            )
        }

        Text(
            text = info.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = AppText,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = info.sig,
            fontSize = 14.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = info.chords,
            fontSize = 15.sp,
            color = AppText,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = info.relative,
            fontSize = 13.sp,
            color = AppMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Text(
            text = info.neighbors,
            fontSize = 13.sp,
            color = AppMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// ---------------------------------------------------------------- METRO VIEW
@Composable
fun MetroView(
    audioSynth: AudioSynthesizer,
    modifier: Modifier = Modifier
) {
    val sigs = mapOf(
        "2/4" to (2 to listOf(0)),
        "3/4" to (3 to listOf(0)),
        "4/4" to (4 to listOf(0)),
        "6/8" to (6 to listOf(0, 3))
    )

    var bpm by remember { mutableIntStateOf(100) }
    var selectedSig by remember { mutableStateOf("4/4") }
    var isRunning by remember { mutableStateOf(false) }
    var currentBeat by remember { mutableIntStateOf(-1) }

    val (beats, accents) = sigs[selectedSig] ?: (4 to listOf(0))
    val taps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(isRunning, bpm, selectedSig) {
        if (!isRunning) return@LaunchedEffect

        var beatIdx = 0
        while (isRunning) {
            val k = beatIdx % beats
            currentBeat = k
            audioSynth.playClick(isAccent = k in accents)
            beatIdx++

            val intervalMs = (60.0 / bpm * 1000).toLong()
            delay(intervalMs)
        }
    }

    val markingText = remember(bpm) {
        when {
            bpm < 60 -> "Largo"
            bpm < 76 -> "Adagio"
            bpm < 108 -> "Andante"
            bpm < 120 -> "Moderato"
            bpm < 168 -> "Allegro"
            else -> "Presto"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = bpm.toString(),
            fontSize = 84.sp,
            fontWeight = FontWeight.Bold,
            color = AppText
        )

        Text(
            text = markingText,
            fontSize = 16.sp,
            color = AppMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Beat Dots Card
        Card(
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            BeatDots(
                beats = beats,
                currentBeat = if (isRunning) currentBeat else -1,
                accents = accents,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // BPM Slider
        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.roundToInt() },
            valueRange = 40f..220f,
            colors = SliderDefaults.colors(
                thumbColor = AppAccent,
                activeTrackColor = AppAccent,
                inactiveTrackColor = AppSurface2
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Step adjustment buttons (-5, -1, +1, +5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("-5" to -5, "-1" to -1, "+1" to 1, "+5" to 5).forEach { (label, delta) ->
                PillButton(
                    text = label,
                    onClick = { bpm = (bpm + delta).coerceIn(40, 220) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time Signature chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sigs.keys.forEach { sigKey ->
                ChipButton(
                    text = sigKey,
                    isSelected = selectedSig == sigKey,
                    onClick = {
                        selectedSig = sigKey
                        currentBeat = -1
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Start/Stop and TAP buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillButton(
                text = if (isRunning) "Stop" else "Avvia",
                isHot = isRunning,
                onClick = {
                    if (isRunning) {
                        isRunning = false
                        currentBeat = -1
                    } else {
                        currentBeat = 0
                        isRunning = true
                    }
                },
                modifier = Modifier.weight(1.3f)
            )

            PillButton(
                text = "Tap",
                onClick = {
                    val now = System.currentTimeMillis()
                    if (taps.isNotEmpty() && now - taps.last() > 2000) {
                        taps.clear()
                    }
                    taps.add(now)
                    if (taps.size >= 5) taps.removeAt(0)
                    if (taps.size >= 2) {
                        val intervalAvg = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
                        val calcBpm = (60000.0 / intervalAvg).roundToInt().coerceIn(40, 220)
                        bpm = calcBpm
                    }
                },
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}
