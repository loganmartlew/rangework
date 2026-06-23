package com.loganmartlew.rangework.android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SettingsSubheader
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono

/** Public MCP endpoint that AI clients connect to. */
private const val CONNECTION_URL = "https://mcp.rangework.app/mcp"

private data class SetupClient(
    val label: String,
    val steps: List<String>,
)

private val setupClients = listOf(
    SetupClient(
        label = "Claude",
        steps = listOf(
            "Open Claude (claude.ai or the desktop app) and go to Settings, then Connectors.",
            "Choose Add custom connector.",
            "Paste the Rangework connection URL above as the server URL.",
            "Select Connect and sign in with the same account you use for Rangework.",
            "Approve access and Rangework's tools will be available in your chats.",
        ),
    ),
    SetupClient(
        label = "ChatGPT",
        steps = listOf(
            "MCP connectors require a paid ChatGPT plan (Plus, Pro, or Team).",
            "Open Settings then Connectors.",
            "Add a new connector and paste the Rangework connection URL above.",
            "Sign in with the same account you use for Rangework.",
            "Approve access, then enable the Rangework connector in your chat.",
        ),
    ),
    SetupClient(
        label = "Other",
        steps = listOf(
            "In your AI client, find the option to add a custom or remote MCP connector.",
            "Use the connection URL above as the server URL.",
            "Sign in with your Rangework account when prompted.",
            "Once connected, the assistant can read your clubs, units, and sessions, and create new ones.",
        ),
    ),
)

private val examplePrompts = listOf(
    "Build me a 45-minute range session focused on wedge distance control, using the clubs in my bag.",
    "Create a putting unit with a gate drill and a lag-putting ladder, then add it to a new pre-round warm-up session.",
    "Look at my existing sessions and suggest a new one that fills a gap in my short game.",
)

@Composable
internal fun AiSessionPlansScreen() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    fun copy(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    ScrollableScreen {
        // Intro
        Text(
            text = "Build practice units and sessions just by chatting. Connect an AI " +
                "assistant like Claude or ChatGPT to your Rangework account, describe the " +
                "practice you want, and the plans it creates appear right here in your " +
                "Units and Sessions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Connection URL
        SettingsSubheader("Connection URL")
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = CONNECTION_URL,
                    style = RangeworkMono.small,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { copy(CONNECTION_URL) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy connection URL",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Setup
        SettingsSubheader("Set up")
        var selectedClient by remember { mutableIntStateOf(0) }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            setupClients.forEachIndexed { index, client ->
                SegmentedButton(
                    selected = selectedClient == index,
                    onClick = { selectedClient = index },
                    shape = SegmentedButtonDefaults.itemShape(index, setupClients.size),
                ) {
                    Text(client.label)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            setupClients[selectedClient].steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = RangeworkMono.small,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            text = "Sign in with the same account you use for Rangework. The assistant can " +
                "read your clubs, units, and sessions, and create new ones on your behalf.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Example prompts
        SettingsSubheader("Try a prompt")
        Text(
            text = "Copy one of these into your AI client to get started. The assistant " +
                "checks your clubs and plans, then creates the result. Pull to refresh " +
                "Units or Sessions to see it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            examplePrompts.forEach { prompt ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                        )
                        IconButton(onClick = { copy(prompt) }) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy prompt",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
