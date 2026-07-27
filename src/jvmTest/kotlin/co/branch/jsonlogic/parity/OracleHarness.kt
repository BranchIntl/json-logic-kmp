package co.branch.jsonlogic.parity

import co.branch.jsonlogic.fixtures.EmbeddedFixtures
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import io.github.jamsesso.jsonlogic.utils.JsonValueExtractor
import kotlinx.serialization.json.JsonElement
import com.google.gson.JsonElement as GsonElement

/**
 * One fixture case prepared for the Java engine the way that engine's own fixture suites prepare it
 * (`FixtureTests`/`ErrorFixtureTests`): the rule is the Gson rendering of the rule element, handed to
 * `JsonLogic.apply(String, Object)` for it to re-parse itself, and the data is
 * [JsonValueExtractor.extract]'s plain-Java projection of the data element — every number a Double,
 * objects as HashMaps, JSON null as Java null.
 *
 * [ruleElement] is kept so the gate can check that a case lines up with the one this library's own
 * loader produced at the same ordinal before diffing the two.
 */
internal class OracleCase(val rule: String, val ruleElement: GsonElement, val data: Any?)

/**
 * Reads the fixture corpus for the Java engine, replicating `FixtureTests.readFixtures`: parse the
 * whole file with Gson, walk the top-level array in order, and skip every element that is not itself
 * an array — which is how both engines' loaders pass over the section-header strings.
 *
 * The text is the same bytes the Java suites read from their test resources: the `generateFixtures`
 * task embeds the files under `fixtures/`, and `:parity` points its test resources at that same
 * directory.
 */
internal object OracleFixtures {

    fun valueCases(): List<OracleCase> = read("fixtures.json")

    fun errorCases(): List<OracleCase> = read("error-fixtures.json")

    private fun read(fileName: String): List<OracleCase> {
        val text = EmbeddedFixtures.files[fileName]
            ?: error("Fixture file '$fileName' was not embedded by the generateFixtures Gradle task.")
        val root: JsonArray = gsonParse(text).asJsonArray
        val cases = mutableListOf<OracleCase>()

        for (element in root) {
            if (!element.isJsonArray) continue

            val array = element.asJsonArray
            cases += OracleCase(
                rule = array.get(0).toString(),
                ruleElement = array.get(0),
                data = JsonValueExtractor.extract(array.get(1)),
            )
        }

        return cases
    }
}

/**
 * Prepares a case this library's side generated for the Java engine, over the same Gson and
 * [JsonValueExtractor] route the fixture cases take.
 */
internal fun oracleCaseOf(rule: JsonElement, data: JsonElement): OracleCase {
    val ruleElement = gsonParse(rule.toString())

    return OracleCase(
        rule = ruleElement.toString(),
        ruleElement = ruleElement,
        data = JsonValueExtractor.extract(gsonParse(data.toString())),
    )
}

/** Gson's lenient parse, the entry point both of the Java engine's own paths into JSON use. */
@Suppress("DEPRECATION")
internal fun gsonParse(json: String): GsonElement = JsonParser().parse(json)
