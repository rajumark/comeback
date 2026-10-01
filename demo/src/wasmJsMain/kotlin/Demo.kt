@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.js.ExperimentalWasmJsInterop
import io.github.rajumark.hoverfly.comeback.Comeback

// Website live demo: docs/demo/worker.js calls load() once, then run() per input; run() returns JSON.

private fun q(s: String) = buildString {
    append('"')
    for (c in s) when (c) {
        '"' -> append("\\\""); '\\' -> append("\\\\")
        else -> if (c < ' ') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
    }
    append('"')
}

private var instance: Comeback? = null

private fun model(): Comeback = instance ?: Comeback().also { instance = it }

/** Loads the bundled model and warms it up. */
@JsExport
fun load() {
    model()
}

/** Suggested replies, best first: [{text, confidence}]. */
@JsExport
fun run(input: String, option: String): String =
    model().replies(input, 5).joinToString(",", "[", "]") { "{\"text\":${q(it.text)},\"confidence\":${it.confidence}}" }

fun main() {}
