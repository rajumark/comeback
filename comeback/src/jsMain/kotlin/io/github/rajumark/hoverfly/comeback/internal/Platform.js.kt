package io.github.rajumark.hoverfly.comeback.internal

internal actual fun nfkc(s: String): String = s.asDynamic().normalize("NFKC") as String
