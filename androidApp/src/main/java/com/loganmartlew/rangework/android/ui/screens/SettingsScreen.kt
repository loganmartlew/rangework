package com.loganmartlew.rangework.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.BuildConfig
import com.loganmartlew.rangework.android.ui.AuthUiState
import com.loganmartlew.rangework.android.ui.SettingsUiState
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SettingsActionRow
import com.loganmartlew.rangework.android.ui.components.SettingsReadOnlyRow
import com.loganmartlew.rangework.android.ui.components.SettingsSectionHeader
import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.SpeedUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    authUiState: AuthUiState,
    settingsUiState: SettingsUiState,
    onSignOut: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectDistanceUnit: (DistanceUnit) -> Unit,
    onSelectSpeedUnit: (SpeedUnit) -> Unit,
    onSetClubEnabled: (String, Boolean) -> Unit,
) {
    val signedInState = authUiState.authState as? AuthState.SignedIn
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access your planning workspace.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ScrollableScreen {
        SettingsSectionHeader("Account")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Signed in as",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = signedInState?.userEmail ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider()
                TextButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.padding(vertical = 4.dp),
                    enabled = !authUiState.actionInProgress,
                ) {
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        SettingsSectionHeader("Appearance")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                    val labels = listOf("System", "Light", "Dark")
                    options.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settingsUiState.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            Text(labels[index])
                        }
                    }
                }
            }
        }

        SettingsSectionHeader("Units")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val distOptions = listOf(DistanceUnit.YARDS, DistanceUnit.METERS)
                    val distLabels = listOf("Yards", "Meters")
                    distOptions.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settingsUiState.measurementPreferences.distanceUnit == unit,
                            onClick = { onSelectDistanceUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, distOptions.size),
                            enabled = !settingsUiState.isWorking && settingsUiState.dataConfigured,
                        ) {
                            Text(distLabels[index])
                        }
                    }
                }
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val speedOptions = listOf(
                        SpeedUnit.MILES_PER_HOUR,
                        SpeedUnit.KILOMETRES_PER_HOUR,
                        SpeedUnit.METRES_PER_SECOND,
                    )
                    val speedLabels = listOf("mph", "km/h", "m/s")
                    speedOptions.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settingsUiState.measurementPreferences.speedUnit == unit,
                            onClick = { onSelectSpeedUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, speedOptions.size),
                            enabled = !settingsUiState.isWorking && settingsUiState.dataConfigured,
                        ) {
                            Text(speedLabels[index])
                        }
                    }
                }

                if (!settingsUiState.dataConfigured) {
                    Text(
                        text = "Unit preferences are not available in this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingsSectionHeader("Clubs")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!settingsUiState.dataConfigured) {
                    Text(
                        text = "Club preferences are not available in this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else if (settingsUiState.clubCatalog.isEmpty()) {
                    Text(
                        text = "Loading clubs…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    val grouped = settingsUiState.clubCatalog.groupBy(Club::category)
                    val categoryOrder = listOf(
                        ClubCategory.WOOD,
                        ClubCategory.HYBRID,
                        ClubCategory.IRON,
                        ClubCategory.WEDGE,
                        ClubCategory.PUTTER,
                    )
                    val categoryLabels = mapOf(
                        ClubCategory.WOOD to "Woods",
                        ClubCategory.HYBRID to "Hybrids",
                        ClubCategory.IRON to "Irons",
                        ClubCategory.WEDGE to "Wedges",
                        ClubCategory.PUTTER to "Putter",
                    )
                    categoryOrder.forEach { category ->
                        val clubs = grouped[category] ?: return@forEach
                        Text(
                            text = categoryLabels[category] ?: category.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                        clubs.forEach { club ->
                            val enabled = club.code in settingsUiState.enabledClubCodes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = club.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onSetClubEnabled(club.code, it) },
                                    enabled = !settingsUiState.isWorking,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        SettingsSectionHeader("About")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                SettingsReadOnlyRow(label = "Version", value = BuildConfig.VERSION_NAME)
                HorizontalDivider()
                SettingsActionRow(label = "Help", onClick = { showHelpSheet = true })
                HorizontalDivider()
                SettingsActionRow(
                    label = "Feedback",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@rangework.app")
                            putExtra(Intent.EXTRA_SUBJECT, "Rangework Feedback")
                        }
                        context.startActivity(intent)
                    },
                )
                HorizontalDivider()
                SettingsActionRow(label = "Privacy", onClick = { showPrivacySheet = true })
            }
        }
    }

    if (showHelpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Help", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Rangework helps you plan focused golf practice sessions. Build reusable units — each with ordered instructions and a ball count — then combine them into sessions. Use the Overview screen to review your plan at a glance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "For additional support, use the Feedback option in Settings to reach us by email.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Privacy", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Rangework stores your practice data securely in Supabase, associated with your Google account. Your data is not sold or shared with third parties. Sign out at any time to stop syncing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "A full privacy policy will be available at launch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
