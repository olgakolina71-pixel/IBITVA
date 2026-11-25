package com.ibitva.studio

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Studio() }
    }
}

private val dark = Color(0xFF0A0D12)
private val pane = Color(0xFF161B24)
private val violet = Color(0xFFB78CFF)

@Composable
fun Studio() {
    val ctx = LocalContext.current
    var bpm by remember { mutableIntStateOf(124) }
    var selected by remember { mutableIntStateOf(0) }
    var notes by remember {
        mutableStateOf(listOf(
            Note(0,72), Note(2,76), Note(4,79), Note(6,76),
            Note(8,72), Note(10,74), Note(12,79), Note(14,81)
        ))
    }
    val labels = listOf("LEAD", "BASS", "PAD", "BEAT")
    val tint = listOf(violet, Color(0xFF61E6CC), Color(0xFFFFB76B), Color(0xFFFF728C))

    MaterialTheme(colorScheme = darkColorScheme(primary = violet, background = dark, surface = pane)) {
        Column(
            modifier = Modifier.fillMaxSize().background(dark).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("IBITVA", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text("STUDIO / CREATE", fontSize = 10.sp, color = violet, letterSpacing = 2.sp)
                }
                Text("v0.1", color = Color.Gray)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = pane)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("TEMPO", fontSize = 10.sp, color = Color.Gray)
                        Text("$bpm BPM", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = pane)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("LOOP", fontSize = 10.sp, color = Color.Gray)
                        Text("4 BARS / 16 STEPS", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = it.toInt() },
                valueRange = 70f..180f,
                colors = SliderDefaults.colors(thumbColor = violet, activeTrackColor = violet)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                labels.forEachIndexed { i, s ->
                    FilterChip(
                        selected = selected == i,
                        onClick = { selected = i },
                        label = { Text(s, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = tint[i].copy(alpha = .2f),
                            selectedLabelColor = tint[i]
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = pane),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Text("PIANO ROLL", color = Color.Gray, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(5.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.width(32.dp)) {
                            (0..11).forEach { row ->
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(noteName(83 - row), fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            (0..11).forEach { row ->
                                Row(modifier = Modifier.weight(1f)) {
                                    (0..15).forEach { step ->
                                        val pitch = 83 - row
                                        val active = notes.any { it.step == step && it.pitch == pitch }
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight().padding(1.dp)
                                                .background(
                                                    if (active) tint[selected]
                                                    else if (step % 4 == 0) Color(0xFF2A303D)
                                                    else Color(0xFF1D232D),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .clickable {
                                                    notes = if (active) notes.filterNot { it.step == step && it.pitch == pitch }
                                                    else notes + Note(step, pitch, 1, selected % 3)
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text("Tap cells to toggle notes • 1 octave shown", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 7.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { SynthEngine.play(notes, bpm) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = violet, contentColor = Color(0xFF21132D)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("▶  PLAY", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = { notes = emptyList() }, modifier = Modifier.weight(.65f), shape = RoundedCornerShape(12.dp)) {
                    Text("CLEAR")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val data = JSONObject().put("bpm", bpm).put("notes", JSONArray().apply {
                            notes.forEach { put(JSONObject().put("step", it.step).put("pitch", it.pitch).put("length", it.length).put("wave", it.wave)) }
                        })
                        File(ctx.filesDir, "ibitva-project.json").writeText(data.toString())
                        Toast.makeText(ctx, "Project saved on this device", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("SAVE") }

                OutlinedButton(
                    onClick = {
                        val duration = maxOf(4.0, notes.maxOfOrNull { it.step + it.length }?.let { it * 60.0 / bpm / 4 } ?: 4.0)
                        val file = File(ctx.cacheDir, "IBITVA-$bpm-BPM.wav")
                        file.writeBytes(SynthEngine.wav(SynthEngine.render(notes, bpm, duration)))
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".provider", file)
                        val intent = Intent(Intent.ACTION_SEND).setType("audio/wav")
                            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        ctx.startActivity(Intent.createChooser(intent, "Share WAV file"))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("EXPORT WAV") }
            }
            Text("OFFLINE AUDIO • SYNTH • NO ACCOUNT", color = Color(0xFF596273), fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

fun noteName(p: Int): String {
    val names = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    return names[p % 12] + (p / 12 - 1)
}
