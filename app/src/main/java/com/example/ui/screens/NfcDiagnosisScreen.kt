package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NfcHardwareInfo
import com.example.ui.components.openNfcSettings
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun NfcDiagnosisScreen(
    hardwareInfo: NfcHardwareInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Nfc, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "NFC Hardware Diagnosis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(text = "Hardware compatibility & feature audit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Check", fontSize = 12.sp)
            }
        }

        // Main Hardware Verdict Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess.copy(alpha = 0.12f)
                else if (hardwareInfo.hasNfc) StatusWarning.copy(alpha = 0.12f)
                else StatusError.copy(alpha = 0.12f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess.copy(alpha = 0.2f)
                                else if (hardwareInfo.hasNfc) StatusWarning.copy(alpha = 0.2f)
                                else StatusError.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) Icons.Default.CheckCircle
                            else if (hardwareInfo.hasNfc) Icons.Default.Nfc
                            else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess
                            else if (hardwareInfo.hasNfc) StatusWarning
                            else StatusError,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = when {
                                !hardwareInfo.hasNfc -> "NFC Hardware Missing"
                                !hardwareInfo.isEnabled -> "NFC Disabled in Settings"
                                else -> "NFC Hardware Ready & Functional"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                !hardwareInfo.hasNfc -> StatusError
                                !hardwareInfo.isEnabled -> StatusWarning
                                else -> StatusSuccess
                            }
                        )
                        Text(
                            text = when {
                                !hardwareInfo.hasNfc -> "This phone model has no physical NFC chipset."
                                !hardwareInfo.isEnabled -> "Turn on NFC in device settings to begin reading."
                                else -> "All NFC reading, writing, and EMV scanning features are active."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hardwareInfo.hasNfc && !hardwareInfo.isEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { openNfcSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusWarning, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable NFC in Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hardware Capability Matrix
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "System & Hardware Specifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                DiagRow(label = "Device Model", value = hardwareInfo.deviceModel, isOk = true)
                DiagRow(label = "Android OS Version", value = "Android ${hardwareInfo.androidVersion}", isOk = true)
                DiagRow(label = "Physical NFC Controller", value = if (hardwareInfo.hasNfc) "Detected" else "Not Present", isOk = hardwareInfo.hasNfc)
                DiagRow(label = "NFC Adapter State", value = if (hardwareInfo.isEnabled) "Enabled (Active)" else "Disabled", isOk = hardwareInfo.isEnabled)
                DiagRow(label = "Host Card Emulation (HCE)", value = if (hardwareInfo.hasHce) "Supported" else "Unsupported", isOk = hardwareInfo.hasHce)
                DiagRow(label = "Android Beam Feature", value = if (hardwareInfo.hasBeam) "Supported" else "N/A", isOk = hardwareInfo.hasBeam)
            }
        }

        // Supported Standards Card
        if (hardwareInfo.supportedTechSummary.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Supported RF Protocols & Chip Types", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    hardwareInfo.supportedTechSummary.forEach { tech ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tech, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Tips for Scanning Card & Tags
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tips for Optimal NFC Reading", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• Antenna Location: Most Android phones place the NFC coil near the top camera bump or upper center of the back panel.\n\n" +
                            "• Contactless Bank Cards: Align the card chip directly against the upper rear of the phone for 1-2 seconds until vibration triggers.\n\n" +
                            "• Phone Cases: Thick metal cases, MagSafe rings, or RFID blocking wallets can block radio signals. Remove thick cases if scans fail.\n\n" +
                            "• Reader Mode: This app uses modern Android ISO-DEP ReaderMode with automatic polling for Visa, Mastercard, NAPAS, and standard tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isOk) MaterialTheme.colorScheme.onSurface else StatusError
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (isOk) StatusSuccess else StatusError,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
