package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.loganmartlew.rangework.shared.model.Club

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClubPickerField(
    label: String,
    selectedCode: String?,
    clubCatalog: List<Club>,
    enabledClubCodes: Set<String>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    // When the instruction sets no Club of its own, the unit's default Club is
    // shown as greyed placeholder text so the inheritance is legible at a glance.
    placeholderClubCode: String? = null,
    // Label for the "clear" menu entry; instruction rows use "Use default".
    noneLabel: String = "None",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedClub = selectedCode?.let { code -> clubCatalog.firstOrNull { it.code == code } }
    val disabledSelected = selectedClub != null && selectedCode !in enabledClubCodes
    val pickerClubs = buildList {
        if (disabledSelected) add(selectedClub!!)
        addAll(clubCatalog.filter { it.code in enabledClubCodes })
    }
    val displayValue = when {
        selectedClub != null && disabledSelected -> "${selectedClub.displayName} (disabled)"
        selectedClub != null -> selectedClub.displayName
        else -> ""
    }
    val placeholderClub = placeholderClubCode
        ?.takeIf { selectedClub == null }
        ?.let { code -> clubCatalog.firstOrNull { it.code == code } }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            readOnly = true,
            value = displayValue,
            onValueChange = {},
            label = { Text(label) },
            placeholder = placeholderClub?.let { club ->
                { Text("${club.displayName} (default)") }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    onSelect("")
                    expanded = false
                },
            )
            pickerClubs.forEach { club ->
                val isDisabled = club.code !in enabledClubCodes
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (isDisabled) "${club.displayName} (disabled)" else club.displayName,
                            color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = {
                        onSelect(club.code)
                        expanded = false
                    },
                )
            }
        }
    }
}
