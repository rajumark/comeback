package io.github.rajumark.hoverfly.comeback.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.comeback.Comeback
import io.github.rajumark.hoverfly.comeback.SmartReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ComebackTheme { ComebackScreen() } }
    }
}

private val EXAMPLES = listOf(
    "Are you coming tonight?", "I got the job!", "Running 10 minutes late", "Can you call me?",
    "Happy birthday!", "I have a fever", "Pizza or burgers?", "Thanks for your help",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val replies: List<SmartReply>, val micros: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComebackScreen() {
    val context = LocalContext.current.applicationContext

    // Loading reads ~5 MB: do it once, off the main thread.
    val comeback by produceState<Comeback?>(null) {
        value = withContext(Dispatchers.Default) { Comeback(context) }
        awaitDispose { value?.close() }
    }
    var message by remember { mutableStateOf(EXAMPLES[0]) }
    var sent by remember { mutableStateOf<String?>(null) }

    val result by produceState<Result?>(null, comeback, message) {
        val c = comeback ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val r = c.replies(message)
            Result(r, (System.nanoTime() - t0) / 1000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Comeback") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it; sent = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message you received") },
                trailingIcon = {
                    if (message.isNotEmpty()) IconButton(onClick = { message = ""; sent = null }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                },
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
                            r.replies.joinToString("  ·  ") { String.format(Locale.ROOT, "%.0f%%", it.confidence * 100) } +
                                "  ·  ${r.micros} µs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text("Try", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXAMPLES.forEach { SuggestionChip(onClick = { message = it; sent = null }, label = { Text(it) }) }
            }
            Text(
                "Runs on this device. No network, no permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
fun ComebackTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
