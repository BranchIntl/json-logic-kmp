package co.branch.jsonlogic.playground.share

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class SharedState(val rule: String, val data: String)

/**
 * The payload sits in the fragment so it never reaches the server, and is base64url because JSON is
 * dense in the characters percent-encoding triples in size.
 *
 * Decoding is total: any malformed fragment yields null, for the caller to fall back on.
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
