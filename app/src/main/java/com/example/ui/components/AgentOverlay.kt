package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AgentOverlay(
    isOperatorActive: Boolean,
    onToggleOperator: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Agent Pointer (Simulated)
        if (isOperatorActive) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Yellow)
                    .align(Alignment.Center) // For now, static center
            )
        }

        // Control Panel
        Column(modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
            Text("AI Operator", style = MaterialTheme.typography.labelSmall)
            Switch(checked = isOperatorActive, onCheckedChange = onToggleOperator)
        }
    }
}
