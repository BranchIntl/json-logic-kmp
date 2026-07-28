package co.branch.jsonlogic.playground.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.playground.EvalOutcome
import co.branch.jsonlogic.playground.editor.highlightJson
import co.branch.jsonlogic.playground.theme.LocalMonospaceStyle
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
private val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

@Composable
fun ResultContent(outcome: EvalOutcome, modifier: Modifier = Modifier) {
    when (outcome) {
        EvalOutcome.Empty -> Placeholder(modifier)
        is EvalOutcome.Success -> SuccessBody(outcome.value, modifier)
        is EvalOutcome.Failure -> FailureBody(outcome, modifier)
    }
}

@Composable
private fun Placeholder(modifier: Modifier) {
    Box(modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Enter a rule to see its result.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SuccessBody(value: JsonElement, modifier: Modifier) {
    SelectionContainer {
        Box(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            val colors = LocalPlaygroundColors.current.syntax
            val pretty = prettyPrint(value)
            Text(
                text = remember(pretty, colors) { highlightJson(pretty, colors) },
                style = LocalMonospaceStyle.current,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FailureBody(failure: EvalOutcome.Failure, modifier: Modifier) {
    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = failure.kind.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = failure.detail,
                style = LocalMonospaceStyle.current,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            failure.jsonPath?.let { path ->
                Text(
                    text = "at $path",
                    style = LocalMonospaceStyle.current,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Infinity and NaN arrive as unquoted literals, since JSON has no token for them, and are printed
 * as-is because that is what the engine returned.
 */
fun prettyPrint(value: JsonElement): String =
    try {
        PrettyJson.encodeToString(JsonElement.serializer(), value)
    } catch (e: Throwable) {
        value.toString()
    }

/**
 * Worth showing because the engine normalizes every number to a Double: `number` beside `1.0`
 * explains a result that would otherwise read as a formatting bug.
 */
fun JsonElement.typeName(): String = when (this) {
    JsonNull -> "null"
    is JsonObject -> "object"
    is JsonArray -> "array"
    is JsonPrimitive -> when {
        isString -> "string"
        content == "true" || content == "false" -> "boolean"
        else -> "number"
    }
}
