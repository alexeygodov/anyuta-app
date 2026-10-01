package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check

@Composable
internal fun ScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ordinary cards always use the same neutral surface and primary text pair. */
@Composable
internal fun neutralCardColors() = androidx.compose.material3.CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
internal fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.FilterChip(
        selected = selected, onClick = onClick, label = label, modifier = modifier, enabled = enabled,
        leadingIcon = if (selected) { { androidx.compose.material3.Icon(Icons.Outlined.Check, contentDescription = null) } } else null,
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = colors.surface, labelColor = colors.onSurfaceVariant,
            selectedContainerColor = colors.primaryContainer, selectedLabelColor = colors.onPrimaryContainer,
            selectedLeadingIconColor = colors.onPrimaryContainer,
        ),
        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
            enabled = enabled, selected = selected, borderColor = colors.outline,
            selectedBorderColor = colors.primary, selectedBorderWidth = 1.dp,
        ),
    )
}
