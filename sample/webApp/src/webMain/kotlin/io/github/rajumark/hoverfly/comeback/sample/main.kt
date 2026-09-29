package io.github.rajumark.hoverfly.comeback.sample

import io.github.rajumark.hoverfly.comeback.Comeback
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/** "Kotlin/JS" or "Kotlin/Wasm". */
expect val runtime: String

private val examples = listOf(
    "Are you coming tonight?", "I got the job!", "Running 10 minutes late", "Can you call me?",
    "Happy birthday!", "I have a fever", "Pizza or burgers?", "Thanks for your help",
)

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

fun main() {
    fun el(id: String) = document.getElementById(id) as HTMLElement
    val input = document.getElementById("text") as HTMLInputElement
    el("platform").textContent = "Kotlin Multiplatform · $runtime · io.github.rajumark:comeback:2.0.0"

    val t0 = TimeSource.Monotonic.markNow()
    val comeback = Comeback()
    el("load").textContent = "Model loaded in ${t0.elapsedNow().inWholeMilliseconds} ms"
    var sent: String? = null

    fun render() {
        val mark = TimeSource.Monotonic.markNow()
        val r = comeback.replies(input.value)
        val micros = mark.elapsedNow().inWholeMicroseconds
        val bubbles = StringBuilder()
        if (input.value.isNotBlank()) bubbles.append("<div class=\"b them\">${esc(input.value)}</div>")
        sent?.let { bubbles.append("<div class=\"b me\">${esc(it)}</div>") }
        el("chat").innerHTML = bubbles.toString()
        el("replies").innerHTML = if (sent != null) "" else r.joinToString("") { "<button class=\"reply\">${esc(it.text)}</button>" }
        el("timing").textContent = if (sent != null || r.isEmpty()) "" else
            r.joinToString("  ·  ") { "${(it.confidence * 100).roundToInt()}%" } + "  ·  $micros µs"
        val list = el("replies").querySelectorAll("button")
        for (i in 0 until list.length) {
            val b = list.item(i) as HTMLElement
            b.onclick = { sent = b.textContent; render(); null }
        }
    }

    el("examples").innerHTML = examples.joinToString("") { "<button class=\"chip\">${esc(it)}</button>" }
    val list = el("examples").querySelectorAll("button")
    for (i in 0 until list.length) {
        val b = list.item(i) as HTMLElement
        b.onclick = { input.value = b.textContent ?: ""; sent = null; render(); null }
    }
    input.oninput = { sent = null; render(); null }
    input.value = examples[0]
    render()
}
