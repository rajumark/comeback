package io.github.rajumark.hoverfly.comeback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The public API: README examples, the selection rules, edge cases and latency. */
class ComebackTest {
    private val comeback = ParityTest.testComeback()

    @Test
    fun examples() {
        for (m in listOf("Are you coming tonight?", "I got the job!", "Running 10 minutes late", "Thank you so much!", "I have a fever")) {
            val r = comeback.replies(m)
            println("$m -> ${r.joinToString(" | ") { "${it.text} ${"%.2f".format(it.confidence)}" }}")
            assertEquals(3, r.size)
        }
        val thanks = comeback.replies("Thank you so much!").map { it.text.lowercase() }
        assertTrue(thanks.toString(), thanks.any { "welcome" in it || "problem" in it || "pleasure" in it })
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

    @Test(expected = IllegalArgumentException::class)
    fun badLimit() {
        comeback.replies("hi", limit = 0)
    }

    @Test(expected = IllegalStateException::class)
    fun closed() {
        val c = ParityTest.testComeback()
        c.close()
        c.replies("hello")
    }

    @Test
    fun latency() {
        val texts = listOf("Are you coming tonight?", "I got the job!", "Running 10 minutes late", "ok")
        repeat(2000) { comeback.replies(texts[it % texts.size]) }
        val n = 10_000
        val t0 = System.nanoTime()
        repeat(n) { comeback.replies(texts[it % texts.size]) }
        val ms = (System.nanoTime() - t0) / 1e6 / n
        println("JVM latency: ${"%.3f".format(ms)} ms per message")
        assertTrue("too slow: $ms ms", ms < 20)
    }
}
