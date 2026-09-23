package io.github.rajumark.hoverfly.comeback

import android.content.Context
import io.github.rajumark.hoverfly.comeback.internal.Featurizer
import io.github.rajumark.hoverfly.comeback.internal.Network
import io.github.rajumark.hoverfly.comeback.internal.SentencePiece
import java.io.Closeable
import java.io.InputStream

/**
 * On-device smart replies: give it the message you received, get back short replies to tap.
 *
 * ```
 * Comeback(context).use { comeback ->
 *     comeback.replies("Are you coming tonight?")   // [Yes!, What time?, Sorry, I can't make it]
 * }
 * ```
 *
 * Replies are picked from a fixed, reviewed list of about 1,300 short replies, never generated,
 * so the model cannot write something rude or odd. At most one reply per intent group is
 * returned, so the chips mean different things.
 *
 * Everything runs locally: the ~5 MB model ships inside the library, there is no network,
 * no permission and no dependency. Creating an instance reads the model (tens of ms), so
 * create it off the main thread and keep it around; [replies] takes about a millisecond and is
 * safe to call from several threads.
 */
public class Comeback internal constructor(open: (String) -> InputStream) : Closeable {

    /** Loads the model bundled in the library's assets. */
    public constructor(context: Context) : this({ name -> context.assets.open("$ASSET_DIR/$name") })

    private class Entry(val text: String, val group: Int, val hidden: Boolean)

    private var network: Network? = open("comeback.bin").use { Network(it) }
    private val featurizer = Featurizer(open("spm_pieces.tsv").use { SentencePiece(it) })
    private val entries: List<Entry> = open("replies.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.filter { it.isNotEmpty() }.map { it.split('\t') }.map { Entry(it[0], it[1].toInt(), it[2] == "1") }.toList()
    }

    init {
        check(entries.size == requireNotNull(network).nOutputs) { "replies.tsv does not match comeback.bin" }
        // The first calls run interpreted; pay that here (off the UI thread) instead of on the first message.
        repeat(WARM_UP) { probabilities("warm up the model $it") }
    }

    /**
     * Replies to [message], best first, at most one per intent group.
     *
     * @param limit how many replies to return (1..10). Smart-reply UIs usually show 3.
     * @param minConfidence return nothing when the best reply scores below this (0..1), for
     *   messages where no short reply fits well. The default 0 always returns [limit] replies.
     * @return an empty list for a blank message.
     */
    @JvmOverloads
    public fun replies(message: String, limit: Int = 3, minConfidence: Float = 0f): List<SmartReply> {
        require(limit in 1..MAX_LIMIT) { "limit must be in 1..$MAX_LIMIT" }
        if (message.isBlank()) return emptyList()
        val p = probabilities(message)
        return pick(p, limit).takeIf { it.isNotEmpty() && p[it[0]] >= minConfidence }.orEmpty()
            .map { SmartReply(entries[it].text, p[it]) }
    }

    /** Releases the model (about 7 MB of heap). The instance cannot be used afterwards. */
    override fun close() {
        network = null
    }

    internal fun probabilities(text: String): FloatArray {
        val net = checkNotNull(network) { "Comeback is closed" }
        val f = featurizer.featurize(text)
        return net.probs(f.tokIds, f.gramIds)
    }

    /** Same rule as comeback/select.py: from the best [POOL], skip hidden replies, keep one per group. */
    internal fun pick(p: FloatArray, k: Int): List<Int> {
        val out = ArrayList<Int>(k)
        val used = HashSet<Int>()
        for (i in topK(p, minOf(POOL, p.size))) {
            val e = entries[i]
            if (e.hidden || !used.add(e.group)) continue
            out.add(i)
            if (out.size == k) break
        }
        return out
    }

    internal companion object {
        const val ASSET_DIR = "comeback"
        const val WARM_UP = 20
        const val POOL = 60
        const val MAX_LIMIT = 10

        /** Indices of the k largest values, best first (insertion into a small array; k << n). */
        fun topK(p: FloatArray, k: Int): List<Int> {
            val idx = IntArray(k) { -1 }
            val v = FloatArray(k) { Float.NEGATIVE_INFINITY }
            for (i in p.indices) {
                val x = p[i]
                if (x <= v[k - 1]) continue
                var j = k - 1
                while (j > 0 && v[j - 1] < x) { v[j] = v[j - 1]; idx[j] = idx[j - 1]; j-- }
                v[j] = x; idx[j] = i
            }
            return idx.filter { it >= 0 }
        }
    }
}
