package io.github.rajumark.hoverfly.comeback

/**
 * One suggested reply.
 *
 * @property text the reply to show on a chip, e.g. "Sounds good!".
 * @property confidence the model probability, 0..1. All replies for one message sum to at most 1.
 */
public data class SmartReply(
    val text: String,
    val confidence: Float,
)
