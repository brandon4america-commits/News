package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
fun SwarmPointerOverlay(
    modifier: Modifier = Modifier
) {
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pointerPosition = event.changes.first().position
                    }
                }
            }
    ) {
        // Draw "Swarm" of particles following the pointer
        val particleCount = 20
        repeat(particleCount) { i ->
            val offset = Offset(
                x = pointerPosition.x + (i * 2f * Math.cos(i.toDouble())).toFloat(),
                y = pointerPosition.y + (i * 2f * Math.sin(i.toDouble())).toFloat()
            )
            drawCircle(
                color = Color(0xFF00F2FF).copy(alpha = 0.5f - (i * 0.02f)),
                radius = 10f - (i * 0.2f),
                center = offset
            )
        }
    }
}
