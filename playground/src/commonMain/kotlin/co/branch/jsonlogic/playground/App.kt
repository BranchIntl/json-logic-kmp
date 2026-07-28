package co.branch.jsonlogic.playground

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.playground.editor.JsonEditor
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import co.branch.jsonlogic.playground.theme.PlaygroundTheme
import co.branch.jsonlogic.playground.ui.ExamplesRow
import co.branch.jsonlogic.playground.ui.Header
import co.branch.jsonlogic.playground.ui.Panel
import co.branch.jsonlogic.playground.ui.ResultContent
import co.branch.jsonlogic.playground.ui.StatusLabel
import co.branch.jsonlogic.playground.ui.typeName
import kotlinx.coroutines.delay

/** Below this width the three panels stack into a single scrolling column. */
private val WideLayoutThreshold = 900.dp

@Composable
fun App() {
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()

    // Constructing JsonLogic registers all 34 default operations, so it is built once rather than
    // per keystroke.
    val jsonLogic = remember { JsonLogic() }
    // Opening on the first example rather than an empty page means the first chip reads as selected
    // and there is something to evaluate immediately.
    var rule by remember { mutableStateOf(TextFieldValue(Examples.first().rule)) }
    var data by remember { mutableStateOf(TextFieldValue(Examples.first().data)) }
    var evaluation by remember { mutableStateOf(Evaluation.Blank) }

    LaunchedEffect(rule.text, data.text) {
        delay(120)
        evaluation = evaluate(jsonLogic, rule.text, data.text)
    }

    PlaygroundTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints {
                val wide = maxWidth >= WideLayoutThreshold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (wide) 22.dp else 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header(
                        dark = dark,
                        onToggleTheme = { darkOverride = !dark },
                        compact = !wide,
                    )

                    ExamplesRow(
                        // Derived rather than tracked, so editing either editor deselects the chip
                        // without any bookkeeping.
                        activeLabel = Examples.firstOrNull {
                            it.rule == rule.text && it.data == data.text
                        }?.label,
                        onSelect = { example ->
                            rule = TextFieldValue(example.rule)
                            data = TextFieldValue(example.data)
                        },
                    )

                    Editors(
                        wide = wide,
                        rule = rule,
                        onRuleChange = { rule = it },
                        data = data,
                        onDataChange = { data = it },
                        evaluation = evaluation,
                        modifier = Modifier.weight(1f),
                    )

                    Footnote()
                }
            }
        }
    }
}

@Composable
private fun Editors(
    wide: Boolean,
    rule: TextFieldValue,
    onRuleChange: (TextFieldValue) -> Unit,
    data: TextFieldValue,
    onDataChange: (TextFieldValue) -> Unit,
    evaluation: Evaluation,
    modifier: Modifier = Modifier,
) {
    val rulePanel: @Composable (Modifier) -> Unit = { panelModifier ->
        Panel(
            label = "Rule",
            modifier = panelModifier,
            trailing = { SyntaxStatus(evaluation.ruleValid) },
        ) {
            JsonEditor(rule, onRuleChange, Modifier.fillMaxSize())
        }
    }
    val dataPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        Panel(
            label = "Data",
            modifier = panelModifier,
            trailing = { SyntaxStatus(evaluation.dataValid) },
        ) {
            JsonEditor(data, onDataChange, Modifier.fillMaxSize())
        }
    }
    val resultPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        Panel(
            label = "Result",
            modifier = panelModifier,
            trailing = {
                (evaluation.outcome as? EvalOutcome.Success)?.let { success ->
                    Text(
                        text = success.value.typeName(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) {
            ResultContent(evaluation.outcome, Modifier.fillMaxSize())
        }
    }

    if (wide) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rulePanel(Modifier.weight(3f))
                dataPanel(Modifier.weight(2f))
            }
            resultPanel(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            rulePanel(Modifier.fillMaxWidth().height(250.dp))
            dataPanel(Modifier.fillMaxWidth().height(170.dp))
            resultPanel(Modifier.fillMaxWidth().height(250.dp))
        }
    }
}

@Composable
private fun SyntaxStatus(valid: Boolean) {
    val colors = LocalPlaygroundColors.current

    if (valid) {
        StatusLabel("ok", colors.ok)
    } else {
        StatusLabel("syntax error", MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Footnote() {
    Text(
        text = "Numbers normalize to Double, so 1 evaluates to 1.0. " +
            "Infinity and NaN come back as bare literals, which JSON has no token for.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
