package com.thinhzero.seg.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ScanScreen(
    type: String,
    scannedData: String,
    hasResult: Boolean,
    onCancel: () -> Unit,
    onViewResult: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")

    val radius1 by transition.animateFloat(
        initialValue = 40f, targetValue = 180f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "r1"
    )
    val alpha1 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "a1"
    )
    val radius2 by transition.animateFloat(
        initialValue = 40f, targetValue = 180f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing, delayMillis = 600),
            RepeatMode.Restart
        ),
        label = "r2"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing, delayMillis = 600),
            RepeatMode.Restart
        ),
        label = "a2"
    )
    val radius3 by transition.animateFloat(
        initialValue = 40f, targetValue = 180f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing, delayMillis = 1200),
            RepeatMode.Restart
        ),
        label = "r3"
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing, delayMillis = 1200),
            RepeatMode.Restart
        ),
        label = "a3"
    )

    val modeText = when (type) {
        "emv" -> "Bank Card (EMV)"
        "write" -> "Write Mode"
        else -> "Read Tag"
    }
    val instructionText = when (type) {
        "emv" -> "Hold your contactless card near the device"
        "write" -> "Hold the tag you want to write to"
        else -> "Hold your NFC tag near the device"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Text(
                text = modeText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!hasResult) {
            // Scanning animation
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                val color = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = color.copy(alpha = alpha1), radius = radius1, style = Stroke(width = 3f))
                    drawCircle(color = color.copy(alpha = alpha2), radius = radius2, style = Stroke(width = 3f))
                    drawCircle(color = color.copy(alpha = alpha3), radius = radius3, style = Stroke(width = 3f))
                }
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = instructionText,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Scan complete
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scan Complete!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = scannedData.take(100) + if (scannedData.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (hasResult) {
            Button(
                onClick = onViewResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("View Results", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (!hasResult) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}