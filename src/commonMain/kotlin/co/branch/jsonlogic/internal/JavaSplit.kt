package co.branch.jsonlogic.internal

/**
 * Splits [s] on `'.'` exactly as `java.lang.String.split("\\.")` does, which is how the engine this
 * library ports resolves dotted `var` paths.
 *
 * Two behaviours of the Java method differ from Kotlin's `split` and are relied upon by the port:
 * trailing empty parts are discarded (`"a.b."` yields `["a", "b"]`, and `"."` yields nothing at
 * all), while a subject with no separator is returned whole — including the empty string, which
 * yields `[""]` rather than an empty list.
 */
internal fun javaSplitOnDot(s: String): List<String> {
    if (!s.contains('.')) return listOf(s)
    val parts = s.split('.')
    var size = parts.size
    while (size > 0 && parts[size - 1].isEmpty()) size--
    return if (size == parts.size) parts else parts.subList(0, size)
}
