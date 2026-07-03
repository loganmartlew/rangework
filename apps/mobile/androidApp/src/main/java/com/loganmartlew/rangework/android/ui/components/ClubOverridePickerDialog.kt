package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.Club

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClubOverridePickerDialog(
    currentClubCode: String,
    availableClubs: List<Club>,
    onClubSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Change Club",
                    style = MaterialTheme.typography.headlineSmall,
                )

                LazyColumn(
                    // fill = false keeps the Cancel button on screen when the
                    // club list is taller than the dialog; the list scrolls.
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(availableClubs, key = { it.code }) { club ->
                        val isSelected = club.code == currentClubCode
                        TextButton(
                            onClick = { onClubSelected(club.code) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = club.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
