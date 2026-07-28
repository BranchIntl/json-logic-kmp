package co.branch.jsonlogic.playground.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

const val RepositoryUrl = "https://github.com/BranchIntl/json-logic-kmp"

private const val Subtitle =
    "json-logic-kmp compiled to WebAssembly — the same evaluator that ships to JVM, Android and iOS."

@Composable
fun Header(
    dark: Boolean,
    onToggleTheme: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "JsonLogic Playground",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!compact) {
                Text(
                    text = Subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing()
            IconButton(onClick = onToggleTheme, modifier = Modifier.size(34.dp)) {
                val tint = MaterialTheme.colorScheme.onSurfaceVariant
                if (dark) SunIcon(tint) else MoonIcon(tint)
            }
            TextButton(onClick = { uriHandler.openUri(RepositoryUrl) }) {
                Text("GitHub", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
