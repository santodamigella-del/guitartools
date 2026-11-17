package com.example.guitartools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.guitartools.audio.AudioSynthesizer
import com.example.guitartools.audio.MicInput
import com.example.guitartools.audio.PitchDetector
import com.example.guitartools.model.MusicTheory
import com.example.guitartools.ui.components.AppBg
import com.example.guitartools.ui.components.AppText
import com.example.guitartools.ui.screens.ChipButton
import com.example.guitartools.ui.screens.ChordView
import com.example.guitartools.ui.screens.CircleView
import com.example.guitartools.ui.screens.GiriView
import com.example.guitartools.ui.screens.MetroView
import com.example.guitartools.ui.screens.ScaleView
import com.example.guitartools.ui.screens.StyledSpinner
import com.example.guitartools.ui.screens.TunerView
import com.example.guitartools.ui.screens.VoiceView

class MainActivity : ComponentActivity() {

    private lateinit var mic: MicInput
    private lateinit var audioSynth: AudioSynthesizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mic = MicInput()
        audioSynth = AudioSynthesizer()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBg
                ) {
                    ChitarraLabApp(mic = mic, audioSynth = audioSynth)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mic.stop()
        audioSynth.release()
    }
}

@Composable
fun ChitarraLabApp(
    mic: MicInput,
    audioSynth: AudioSynthesizer
) {
    var hasMicPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            mic.start()
        }
    }

    LaunchedEffect(Unit) {
        val context = mic.hashCode() // Just a trigger
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        onDispose {
            mic.stop()
            audioSynth.stopTone()
        }
    }

    var tuningName by remember { mutableStateOf("Standard (EADGBE)") }
    var a4 by remember { mutableDoubleStateOf(440.0) }
    var currentTab by remember { mutableStateOf("Tuner") }

    var voiceTargetKey by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val stringsRaw = MusicTheory.TUNINGS[tuningName] ?: emptyList()
    val stringPairs = remember(tuningName) {
        if (stringsRaw.isNotEmpty()) {
            stringsRaw.map { it to MusicTheory.noteToSemis(it) }
        } else {
            // Se cromatico, usiamo standard per gli accordi
            (MusicTheory.TUNINGS["Standard (EADGBE)"] ?: emptyList()).map { it to MusicTheory.noteToSemis(it) }
        }
    }

    val fmin = remember(stringPairs) {
        if (stringsRaw.isNotEmpty()) {
            val freqs = stringPairs.map { MusicTheory.semisToFreq(it.second, 440.0) }
            maxOf(25.0f, (freqs.minOrNull()!! * 0.75).toFloat())
        } else 60.0f
    }

    val fmax = remember(stringPairs) {
        if (stringsRaw.isNotEmpty()) {
            val freqs = stringPairs.map { MusicTheory.semisToFreq(it.second, 440.0) }
            minOf(1000.0f, (freqs.maxOrNull()!! * 1.5).toFloat())
        } else 700.0f
    }

    val winLen = remember(fmin) { PitchDetector.windowFor(fmin) }

    val tabs = listOf("Tuner", "Voce", "Accordi", "Giri", "Scale", "Quinte", "Metro")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ChitarraLab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )

                StyledSpinner(
                    selectedText = tuningName,
                    options = MusicTheory.TUNINGS.keys.toList(),
                    onOptionSelected = { tuningName = it },
                    modifier = Modifier.width(200.dp)
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBg)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tabName ->
                    ChipButton(
                        text = tabName,
                        isSelected = currentTab == tabName,
                        onClick = {
                            audioSynth.stopTone()
                            currentTab = tabName
                        }
                    )
                }
            }
        },
        containerColor = AppBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "Tuner" -> TunerView(
                    mic = mic,
                    audioSynth = audioSynth,
                    currentTuningName = tuningName,
                    tuningStrings = if (stringsRaw.isNotEmpty()) stringPairs else emptyList(),
                    a4 = a4,
                    onA4Change = { a4 = it },
                    fmin = fmin,
                    fmax = fmax,
                    winLen = winLen
                )

                "Voce" -> VoiceView(
                    mic = mic,
                    a4 = a4,
                    onOpenGiri = { pc, minor ->
                        voiceTargetKey = pc to minor
                        currentTab = "Giri"
                    }
                )

                "Accordi" -> ChordView(
                    tuningStrings = stringPairs
                )

                "Giri" -> GiriView(
                    tuningStrings = stringPairs,
                    initialPc = voiceTargetKey?.first ?: 0,
                    initialMinor = voiceTargetKey?.second ?: false
                )

                "Scale" -> ScaleView(
                    tuningStrings = stringPairs
                )

                "Quinte" -> CircleView()

                "Metro" -> MetroView(
                    audioSynth = audioSynth
                )
            }
        }
    }
}
