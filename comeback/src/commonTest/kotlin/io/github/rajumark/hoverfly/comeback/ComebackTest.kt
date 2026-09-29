package io.github.rajumark.hoverfly.comeback

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.TimeSource
import kotlin.test.assertFailsWith

/** The public API: README examples, the selection rules, edge cases and latency. */
class ComebackTest {
    private val comeback = ParityTest.testComeback()

    @Test
    fun examples() {
        for (m in listOf("Are you coming tonight?", "I got the job!", "Running 10 minutes late", "Thank you so much!", "I have a fever")) {
            val r = comeback.replies(m)
            println("$m -> ${r.joinToString(" | ") { "${it.text} ${fmt(it.confidence, 2)}" }}")
            assertEquals(3, r.size)
        }
        val thanks = comeback.replies("Thank you so much!").map { it.text.lowercase() }
        assertTrue(thanks.any { "welcome" in it || "problem" in it || "pleasure" in it }, thanks.toString())
    }

    @Test
    fun distinctAndOrdered() {
        for (m in listOf("Want to grab lunch tomorrow?", "Happy birthday!", "Can you send me the file?")) {
            val r = comeback.replies(m, limit = 5)
            assertEquals(5, r.size)
            assertEquals(r.size, r.map { it.text }.toSet().size)
            for (k in 1 until r.size) assertTrue(r[k - 1].confidence >= r[k].confidence)
        }
    }

    @Test
    fun blankAndThreshold() {
        assertTrue(comeback.replies("").isEmpty())
        assertTrue(comeback.replies("   ").isEmpty())
        assertTrue(comeback.replies("Are you coming tonight?", minConfidence = 1f).isEmpty())
        assertEquals(1, comeback.replies("Are you coming tonight?", limit = 1).size)
    }

    @Test
    fun badLimit() {
        assertFailsWith<IllegalArgumentException> { comeback.replies("hi", limit = 0) }
    }

    @Test
    fun closed() {
        val c = Comeback()
        c.close()
        assertFailsWith<IllegalStateException> { c.replies("hello") }
    }

    @Test
    fun latency() {
        val texts = listOf("Are you coming tonight?", "I got the job!", "Running 10 minutes late", "ok")
        repeat(300) { comeback.replies(texts[it % texts.size]) }
        val n = 2000
        val t0 = TimeSource.Monotonic.markNow()
        repeat(n) { comeback.replies(texts[it % texts.size]) }
        val ms = t0.elapsedNow().inWholeNanoseconds / 1e6 / n
        println("latency: ${fmt(ms, 3)} ms per message")
        assertTrue(ms < 200, "too slow: $ms ms") // generous: Kotlin/Native test binaries are unoptimized debug builds
        val l0 = TimeSource.Monotonic.markNow()
        Comeback().close()
        println("load: ${l0.elapsedNow().inWholeMilliseconds} ms")
    }
}
