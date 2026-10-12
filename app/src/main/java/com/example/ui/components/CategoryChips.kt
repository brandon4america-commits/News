package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    categoryCounts: Map<String, Int>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            val count = categoryCounts[category] ?: 0
            val icon = getCategoryIcon(category)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = if (count > 0 && category != "All") "$category ($count)" else category,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) GoogleBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    selectedBorderColor = GoogleBlue
                ),
                modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}")
            )
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when {
        category.contains("All", ignoreCase = true) -> Icons.Default.GridView
        category.contains("Gemini", ignoreCase = true) -> Icons.Default.AutoAwesome
        category.contains("Pixel", ignoreCase = true) -> Icons.Default.Smartphone
        category.contains("Android", ignoreCase = true) -> Icons.Default.PhoneAndroid
        category.contains("Research", ignoreCase = true) -> Icons.Default.Psychology
        category.contains("Workspace", ignoreCase = true) -> Icons.Default.TableChart
        category.contains("Health", ignoreCase = true) -> Icons.Default.Favorite
        category.contains("Sustainability", ignoreCase = true) -> Icons.Default.Eco
        category.contains("Chrome", ignoreCase = true) -> Icons.Default.Language
        else -> Icons.Default.GridView
    }
}
