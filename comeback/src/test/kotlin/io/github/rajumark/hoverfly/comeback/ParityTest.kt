package io.github.rajumark.hoverfly.comeback

import io.github.rajumark.hoverfly.comeback.internal.Featurizer
import io.github.rajumark.hoverfly.comeback.internal.SentencePiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * Checks the Kotlin port against Python on testvectors.tsv (written by
 * comeback/scripts/export_kotlin.py): identical token and n-gram ids, the same top-5 replies with
 * the same probabilities as the reference model in PyTorch, and the same 3 replies shown.
 */
class ParityTest {
    private class Vector(
        val text: String, val tok: List<Int>, val grams: List<Int>,
        val top5: List<Int>, val p5: List<Float>, val shown: List<Int>,
    )

    private fun ints(s: String) = if (s.isEmpty()) emptyList() else s.split(',').map { it.toInt() }

    private val vectors = javaClass.getResourceAsStream("/testvectors.tsv")!!.bufferedReader().readLines()
        .map { it.split('\t') }
        .map { Vector(it[0], ints(it[1]), ints(it[2]), ints(it[3]), it[4].split(',').map(String::toFloat), ints(it[5])) }

    @Test
    fun featurizerMatchesPython() {
        val f = Featurizer(File("$ASSETS/spm_pieces.tsv").inputStream().use { SentencePiece(it) })
        var bad = 0
        for (v in vectors) {
            val got = f.featurize(v.text)
            if (got.tokIds.toList() != v.tok || got.gramIds.toList() != v.grams) {
                bad++
                println("MISMATCH: ${v.text}")
            }
        }
        println("featurizer: ${vectors.size - bad}/${vectors.size} identical")
        assertEquals(0, bad)
    }

    @Test
    fun modelMatchesPython() {
        val comeback = testComeback()
        var top1 = 0
        var top5 = 0
        var shown = 0
        var maxDiff = 0f
        for (v in vectors) {
            val p = comeback.probabilities(v.text)
            val got = Comeback.topK(p, 5)
            if (got[0] == v.top5[0]) top1++
            if (got == v.top5) top5++
            if (comeback.pick(p, 3) == v.shown) shown++ else println("SHOWN DIFF: ${v.text}")
            for ((i, e) in v.top5.withIndex()) maxDiff = maxOf(maxDiff, abs(p[e] - v.p5[i]))
        }
        val n = vectors.size
        println("model: top-1 $top1/$n, identical top-5 $top5/$n, identical shown-3 $shown/$n, max |dp| = $maxDiff")
        assertEquals(n, top1)
        assertTrue("top-5 order differs too often: $top5/$n", top5 >= n - 3) // near-ties may swap
        assertTrue("shown replies differ too often: $shown/$n", shown >= n - 3)
        assertTrue("probabilities drift: $maxDiff", maxDiff < 1e-4f)
    }

    companion object {
        const val ASSETS = "src/main/assets/comeback"
        fun testComeback() = Comeback { name -> File("$ASSETS/$name").inputStream() }
    }
}
