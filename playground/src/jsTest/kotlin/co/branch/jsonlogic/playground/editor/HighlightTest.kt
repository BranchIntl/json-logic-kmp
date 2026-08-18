package co.branch.jsonlogic.playground.editor

import co.branch.jsonlogic.playground.Examples
import co.branch.jsonlogic.playground.OperationGroups
import co.branch.jsonlogic.playground.share.AwkwardStates
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every text the playground can put in front of a reader, plus the half-typed and awkward ones the
 * other suites already collected. The two layers of an editor draw from the same string, so what
 * has to hold is that the spans give the string back.
 */
private val Corpus: List<String> =
    Examples.flatMap { listOf(it.rule, it.data) } +
        OperationGroups.flatMap { group -> group.operations.map { it.snippet } } +
        PartialInputs +
        AwkwardStates.flatMap { listOf(it.rule, it.data) }

class HighlightTest {

    @Test
    fun spansRejoinToTheInput() {
        Corpus.forEach { text ->
            assertEquals(text, highlightSpans(text).joinToString("") { it.text })
        }
    }

    @Test
    fun linesRejoinToTheInput() {
        Corpus.forEach { text ->
            val rejoined = highlightLines(text).joinToString("\n") { line ->
                line.joinToString("") { it.text }
            }

            assertEquals(text, rejoined)
        }
    }

    @Test
    fun anUnterminatedStringKeepsItsKindOnEveryLineItCovers() {
        val lines = highlightLines("{\"a\": \"one\ntwo\nthree")

        assertEquals(3, lines.size)
        assertEquals(
            listOf(JsonTokenKind.Unknown, JsonTokenKind.Unknown, JsonTokenKind.Unknown),
            lines.map { it.last().kind },
        )
        assertEquals(listOf("\"one", "two", "three"), lines.map { it.last().text })
    }

    @Test
    fun aTerminatedStringKeepsItsKindOnEveryLineItCovers() {
        val lines = highlightLines("{\"a\": \"one\ntwo\"}")

        assertEquals(2, lines.size)
        assertEquals(JsonTokenKind.StringValue, lines[0].last().kind)
        assertEquals(JsonTokenKind.StringValue, lines[1].first().kind)
    }

    @Test
    fun aBlankLineIsALineOfItsOwn() {
        val lines = highlightLines("a\n\nb")

        assertEquals(3, lines.size)
        assertEquals(emptyList(), lines[1])
    }

    /** The trailing newline is what the wrapped layer has to match against the textarea. */
    @Test
    fun aTrailingNewlineOpensAnEmptyLastLine() {
        val lines = highlightLines("a\n")

        assertEquals(2, lines.size)
        assertEquals(emptyList(), lines[1])
    }

    @Test
    fun emptyTextIsOneEmptyLine() {
        assertEquals(listOf(emptyList()), highlightLines(""))
    }
}
