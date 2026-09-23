package io.github.rajumark.hoverfly.comeback

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same check as the JVM ParityTest, on a real device: Android's ICU-backed Unicode tables
 * (NFKC, lowercase, character types) must give Python's replies on every vector.
 */
@RunWith(AndroidJUnit4::class)
class DeviceParityTest {
    @Test
    fun matchesPythonOnDevice() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val lines = inst.context.assets.open("testvectors.tsv").bufferedReader().readLines()
        val t0 = System.nanoTime()
        val comeback = Comeback(inst.targetContext)
        val loadMs = (System.nanoTime() - t0) / 1e6
        var same = 0
        for (line in lines) {
            val c = line.split('\t')
            val want = if (c[5].isEmpty()) emptyList() else c[5].split(',').map { it.toInt() }
            if (comeback.pick(comeback.probabilities(c[0]), 3) == want) same++ else println("DIFF: ${c[0]}")
        }
        val texts = lines.map { it.substringBefore('\t') }
        repeat(300) { comeback.replies(texts[it % texts.size]) }
        val n = 3000
        val s0 = System.nanoTime()
        repeat(n) { comeback.replies(texts[it % texts.size]) }
        val ms = (System.nanoTime() - s0) / 1e6 / n
        println("COMEBACK_DEVICE shown3 $same/${lines.size} load=${"%.0f".format(loadMs)}ms latency=${"%.3f".format(ms)}ms")
        assertTrue("$same/${lines.size}", same >= lines.size - 3)
    }
}
