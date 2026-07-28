package co.branch.jsonlogic.playground.share

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Everything a shared link carries: whatever was in the two editors. */
data class SharedState(val rule: String, val data: String)

/**
 * Encodes the editors into a URL fragment, and back.
 *
 * The payload lives in the fragment rather than the query string so it never reaches the server,
 * and each half is base64url rather than percent-encoded: JSON is dense in `{`, `"`, `:` and `,`,
 * every one of which costs three characters percent-encoded, against base64's flat one-third.
 *
 * Decoding is total — any malformed fragment yields null, and the caller opens on its default
 * content instead. A half-readable link is not worth recovering.
 */
@OptIn(ExperimentalEncodingApi::class)
object ShareLink {

    private const val RuleKey = "r"
    private const val DataKey = "d"

    private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    /** The fragment body, with no leading `#`. */
    fun encode(state: SharedState): String =
        "$RuleKey=${codec.encode(state.rule.encodeToByteArray())}" +
            "&$DataKey=${codec.encode(state.data.encodeToByteArray())}"

    /** Accepts the fragment with or without its leading `#`. */
    fun decode(fragment: String): SharedState? {
        val body = fragment.removePrefix("#")
        if (body.isEmpty()) return null

        val parameters = body.split('&').mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null else part.take(separator) to part.substring(separator + 1)
        }.toMap()

        val rule = parameters[RuleKey]?.let(::decodeSegment) ?: return null
        val encodedData = parameters[DataKey]
        val data = if (encodedData == null) "" else decodeSegment(encodedData) ?: return null

        return SharedState(rule, data)
    }

    private fun decodeSegment(encoded: String): String? =
        try {
            codec.decode(encoded).decodeToString()
        } catch (e: IllegalArgumentException) {
            null
        }
}
