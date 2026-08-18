package co.branch.jsonlogic.playground.share

import co.branch.jsonlogic.playground.Examples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShareLinkTest {

    @Test
    fun roundTripsEveryExample() {
        Examples.forEach { example ->
            val state = SharedState(example.rule, example.data)

            assertEquals(state, ShareLink.decode(ShareLink.encode(state)), example.label)
        }
    }

    @Test
    fun roundTripsAwkwardContent() {
        AwkwardStates.forEach { state ->
            assertEquals(state, ShareLink.decode(ShareLink.encode(state)))
        }
    }

    @Test
    fun acceptsTheFragmentWithOrWithoutItsHash() {
        val state = SharedState("""{"==": [1, 1]}""", "{}")
        val encoded = ShareLink.encode(state)

        assertEquals(ShareLink.decode(encoded), ShareLink.decode("#$encoded"))
    }

    @Test
    fun encodingIsUrlSafeAndUnpadded() {
        val encoded = ShareLink.encode(SharedState("""{"substr": ["????>>>>", 0]}""", """{"a": "~~~~"}"""))
        // The keys carry the only '=' the fragment is allowed, so they come off before the scan.
        val payloads = encoded.split('&').map { it.substringAfter('=') }

        assertEquals(2, payloads.size, encoded)
        // '+', '/' and '=' would all have to be percent-escaped again inside a URL.
        payloads.forEach { payload ->
            assertTrue(payload.none { it in "+/=" || it.isWhitespace() }, payload)
        }
        assertTrue(encoded.startsWith("r="), encoded)
    }

    @Test
    fun malformedFragmentsDecodeToNull() {
        val malformed = listOf(
            "",
            "#",
            "r",
            "=abc",
            "d=" + ShareLink.encode(SharedState("x", "y")).substringAfter("r="), // no rule at all
            "r=!!!not-base64!!!",
            "r=" + ShareLink.encode(SharedState("x", "y")).substringAfter("r=").substringBefore("&") + "&d=###",
        )

        malformed.forEach { fragment ->
            assertNull(ShareLink.decode(fragment), "expected null for \"$fragment\"")
        }
    }

    @Test
    fun anAbsentDataParameterMeansEmptyData() {
        val encoded = ShareLink.encode(SharedState("""{"==": [1, 1]}""", "")).substringBefore("&")

        assertEquals(SharedState("""{"==": [1, 1]}""", ""), ShareLink.decode(encoded))
    }
}

/** Text that survives a shared link intact: URL separators, escapes, CRLF, tabs, non-Latin. */
internal val AwkwardStates = listOf(
    SharedState("", ""),
    SharedState("""{"cat": ["a&b=c", "#d"]}""", """{"x": "%20+/="}"""),
    SharedState("""{"var": "ünïcøde ✓ 日本語"}""", """{"ünïcøde": "✓"}"""),
    SharedState("{\n  \"a\": 1\n}", "{\r\n\t\"b\": 2\n}"),
)
