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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.branch.jsonlogic.JsonLogic
import co.branch.jsonlogic.playground.editor.JsonEditor
import co.branch.jsonlogic.playground.share.SharedState
import co.branch.jsonlogic.playground.theme.LocalPlaygroundColors
import co.branch.jsonlogic.playground.theme.PlaygroundTheme
import co.branch.jsonlogic.playground.ui.Chip
import co.branch.jsonlogic.playground.ui.ExamplesRow
import co.branch.jsonlogic.playground.ui.Header
import co.branch.jsonlogic.playground.ui.OperationsReference
import co.branch.jsonlogic.playground.ui.Panel
import co.branch.jsonlogic.playground.ui.ResultContent
import co.branch.jsonlogic.playground.ui.StatusLabel
import co.branch.jsonlogic.playground.ui.typeName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Below this width the three panels stack into a single scrolling column. */
private val WideLayoutThreshold = 900.dp

/**
 * @param initial content restored from a shared link, or null to open on the first example.
 * @param onShare publishes the editors as a link and returns it. Null hides the share control, for
 *   a host with no address bar to publish to.
 */
@Composable
fun App(
    initial: SharedState? = null,
    onShare: ((SharedState) -> String)? = null,
) {
    var darkOverride by remember { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()

    // Built once: the constructor registers all 34 default operations.
    val jsonLogic = remember { JsonLogic() }
    var rule by remember { mutableStateOf(TextFieldValue(initial?.rule ?: Examples.first().rule)) }
    var data by remember { mutableStateOf(TextFieldValue(initial?.data ?: Examples.first().data)) }
    var evaluation by remember { mutableStateOf(Evaluation.Blank) }
    var referenceExpanded by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(rule.text, data.text) {
        delay(120)
        evaluation = evaluate(jsonLogic, rule.text, data.text)
    }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    PlaygroundTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints {
                val wide = maxWidth >= WideLayoutThreshold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Narrow scrolls the page: confining the stacked panels to their own
                        // scroll region buries the result under a screen-tall editor.
                        .then(if (wide) Modifier else Modifier.verticalScroll(rememberScrollState()))
                        .padding(horizontal = if (wide) 22.dp else 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header(
                        dark = dark,
                        onToggleTheme = { darkOverride = !dark },
                        compact = !wide,
                        trailing = {
                            if (onShare != null) {
                                Chip(
                                    text = if (copied) "Copied" else "Share",
                                    onClick = {
                                        val url = onShare(SharedState(rule.text, data.text))
                                        scope.launch {
                                            clipboard.setClipEntry(ClipEntry.withPlainText(url))
                                        }
                                        copied = true
                                    },
                                    selected = true,
                                )
                            }
                        },
                    )

                    ExamplesRow(
                        // Derived, so editing either editor deselects the chip on its own.
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
                        modifier = if (wide) Modifier.weight(1f) else Modifier,
                    )

                    OperationsReference(
                        expanded = referenceExpanded,
                        onToggle = { referenceExpanded = !referenceExpanded },
                        onInsert = { snippet -> rule = rule.insertAtCursor(snippet) },
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
        Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rulePanel(Modifier.fillMaxWidth().height(230.dp))
            dataPanel(Modifier.fillMaxWidth().height(150.dp))
            resultPanel(Modifier.fillMaxWidth().height(220.dp))
        }
    }
}

private fun TextFieldValue.insertAtCursor(snippet: String): TextFieldValue {
    val start = selection.min
    val replaced = text.replaceRange(start, selection.max, snippet)

    return TextFieldValue(replaced, TextRange(start + snippet.length))
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
