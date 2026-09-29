@file:JvmName("ComebackAndroid")

package io.github.rajumark.hoverfly.comeback

import android.content.Context

/** Kept so 1.x code (`Comeback(context)`) still compiles; the model no longer needs a [Context]. */
@Deprecated("The model is bundled without assets now; use Comeback().", ReplaceWith("Comeback()"))
@Suppress("UNUSED_PARAMETER", "FunctionName")
public fun Comeback(context: Context): Comeback = Comeback()
