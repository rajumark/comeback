package io.github.rajumark.hoverfly.comeback.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.comeback.Comeback
import io.github.rajumark.hoverfly.comeback.SmartReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.TimeSource

private val examples = listOf(
    "Are you coming tonight?", "I got the job!", "Running 10 minutes late", "Can you call me?",
    "Happy birthday!", "I have a fever", "Pizza or burgers?", "Thanks for your help",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val replies: List<SmartReply>, val micros: Long)

/** The whole demo: a received message, reply chips to tap. [platform] is shown so screenshots say where they ran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(platform: String) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Loading reads ~5 MB: do it once, off the main thread.
            val comeback by produceState<Comeback?>(null) { value = withContext(Dispatchers.Default) { Comeback() } }
            var message by remember { mutableStateOf(examples[0]) }
            var sent by remember { mutableStateOf<String?>(null) }

            val result by produceState<Result?>(null, comeback, message) {
                val c = comeback ?: return@produceState
                value = withContext(Dispatchers.Default) {
                    val t0 = TimeSource.Monotonic.markNow()
                    val r = c.replies(message)
                    Result(r, t0.elapsedNow().inWholeMicroseconds)
                }
            }

            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Comeback", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Kotlin Multiplatform · $platform · io.github.rajumark:comeback:$COMEBACK_VERSION",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; sent = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message you received") },
                )

                // the conversation
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (message.isNotBlank()) Bubble(message, mine = false)
                    sent?.let { Bubble(it, mine = true) }
                    val r = result
                    when {
                        comeback == null || r == null -> Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        sent == null && r.replies.isNotEmpty() -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                r.replies.forEach { AssistChip(onClick = { sent = it.text }, label = { Text(it.text) }) }
                            }
                            Text(
                                r.replies.joinToString("  ·  ") { "${(it.confidence * 100).roundToInt()}%" } + "  ·  ${r.micros} µs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text("Try", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    examples.forEach { SuggestionChip(onClick = { message = it; sent = null }, label = { Text(it) }) }
                }
                Text(
                    "Runs on this device. No network, no permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Text(
            text,
            Modifier.widthIn(max = 280.dp)
                .background(
                    if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

const val COMEBACK_VERSION = "2.0.0"
