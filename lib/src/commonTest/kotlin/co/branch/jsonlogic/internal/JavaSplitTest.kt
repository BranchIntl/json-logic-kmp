package co.branch.jsonlogic.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class JavaSplitTest {

    @Test
    fun keepsASubjectWithoutSeparatorsWhole() {
        assertEquals(listOf(""), javaSplitOnDot(""))
        assertEquals(listOf("a"), javaSplitOnDot("a"))
        assertEquals(listOf("abc"), javaSplitOnDot("abc"))
    }

    @Test
    fun dropsTrailingEmptyParts() {
        assertEquals(listOf("a"), javaSplitOnDot("a."))
        assertEquals(listOf("a", "b"), javaSplitOnDot("a.b."))
        assertEquals(listOf("a", "b"), javaSplitOnDot("a.b..."))
        assertEquals(emptyList(), javaSplitOnDot("."))
        assertEquals(emptyList(), javaSplitOnDot(".."))
        assertEquals(emptyList(), javaSplitOnDot("....."))
    }

    @Test
    fun keepsLeadingAndInteriorEmptyParts() {
        assertEquals(listOf("", "a"), javaSplitOnDot(".a"))
        assertEquals(listOf("", "", "a"), javaSplitOnDot("..a"))
        assertEquals(listOf("a", "", "b"), javaSplitOnDot("a..b"))
        assertEquals(listOf("", "a", "", "b"), javaSplitOnDot(".a..b"))
        assertEquals(listOf("a", "b", "c"), javaSplitOnDot("a.b.c"))
        assertEquals(listOf("", "a", "b"), javaSplitOnDot(".a.b."))
    }
}
