package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NfcTagDetails
import com.example.data.model.WritePayload
import com.example.data.model.WriteState

@Composable
fun DashboardScreen(
    isWriteMode: Boolean,
    onToggleWriteMode: (Boolean) -> Unit,
    // Read state
    tagDetails: NfcTagDetails?,
    onClearScan: () -> Unit,
    // Write state
    writePayload: WritePayload,
    writeState: WriteState,
    onPayloadChange: (WritePayload) -> Unit,
    onStartWrite: (WritePayload) -> Unit,
    onCancelWrite: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mode Toggle
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SegmentedButton(
                selected = !isWriteMode,
                onClick = { onToggleWriteMode(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Read Mode", fontWeight = if (!isWriteMode) FontWeight.Bold else FontWeight.Normal)
            }
            SegmentedButton(
                selected = isWriteMode,
                onClick = { onToggleWriteMode(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Write Mode", fontWeight = if (isWriteMode) FontWeight.Bold else FontWeight.Normal)
            }
        }

        if (isWriteMode) {
            TagWriterScreen(
                currentPayload = writePayload,
                writeState = writeState,
                onPayloadChange = onPayloadChange,
                onStartWrite = onStartWrite,
                onCancelWrite = onCancelWrite
            )
        } else {
            TagReaderScreen(
                tag = tagDetails,
                onClearScan = onClearScan
            )
        }
    }
}
