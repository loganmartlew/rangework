package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.SettingsUiState
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory

private val CATEGORY_ORDER = listOf(
    ClubCategory.DRIVER,
    ClubCategory.WOOD,
    ClubCategory.HYBRID,
    ClubCategory.IRON,
    ClubCategory.WEDGE,
    ClubCategory.PUTTER,
)

private val CATEGORY_LABELS = mapOf(
    ClubCategory.DRIVER to "Drivers",
    ClubCategory.WOOD to "Woods",
    ClubCategory.HYBRID to "Hybrids",
    ClubCategory.IRON to "Irons",
    ClubCategory.WEDGE to "Wedges",
    ClubCategory.PUTTER to "Putter",
)

@Composable
internal fun ManageClubsScreen(
    settingsUiState: SettingsUiState,
    onSetClubEnabled: (String, Boolean) -> Unit,
    onEnableCommonBag: () -> Unit,
    onDisableAllClubs: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }

    val catalog = settingsUiState.clubCatalog
    val enabledCodes = settingsUiState.enabledClubCodes
    val count = settingsUiState.enabledClubCount

    val filteredCatalog = remember(catalog, searchQuery) {
        if (searchQuery.isBlank()) catalog
        else catalog.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    val groupedClubs = remember(filteredCatalog) {
        filteredCatalog.groupBy(Club::category)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (count.total > 0) "${count.enabled} of ${count.total} enabled" else "Loading…",
                style = RangeworkMono.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                IconButton(onClick = { searchActive = !searchActive }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = if (searchActive) "Close search" else "Search clubs",
                    )
                }
                IconButton(onClick = { overflowExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                    )
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Enable common bag") },
                        onClick = {
                            overflowExpanded = false
                            onEnableCommonBag()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Disable all") },
                        onClick = {
                            overflowExpanded = false
                            onDisableAllClubs()
                        },
                    )
                }
            }
        }

        if (searchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text("Search clubs") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
            )
        }

        when {
            !settingsUiState.dataConfigured -> {
                Text(
                    text = "Club preferences are not available in this build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            catalog.isEmpty() -> {
                Text(
                    text = "Loading clubs…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            filteredCatalog.isEmpty() -> {
                Text(
                    text = "No clubs match your search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    CATEGORY_ORDER.forEach { category ->
                        val clubs = groupedClubs[category] ?: return@forEach
                        item(key = "header_${category.name}") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (CATEGORY_LABELS[category] ?: category.name).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(clubs, key = { it.code }) { club ->
                            val enabled = club.code in enabledCodes
                            val description = if (enabled) "${club.displayName}, enabled" else "${club.displayName}, disabled"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(vertical = 4.dp)
                                    .semantics {
                                        role = Role.Switch
                                        contentDescription = description
                                        toggleableState = if (enabled) ToggleableState.On else ToggleableState.Off
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = club.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onSetClubEnabled(club.code, it) },
                                    enabled = !settingsUiState.isWorking,
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
