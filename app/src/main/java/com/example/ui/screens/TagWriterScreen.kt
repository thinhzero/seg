package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WritePayload
import com.example.data.model.WritePayloadType
import com.example.data.model.WriteState
import com.example.ui.components.RadarPulseScanner
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

data class WriterCategory(
    val type: WritePayloadType,
    val title: String,
    val icon: ImageVector
)

val WRITER_CATEGORIES = listOf(
    WriterCategory(WritePayloadType.TEXT, "Text", Icons.Default.TextFields),
    WriterCategory(WritePayloadType.URI, "URL / Web", Icons.Default.Language),
    WriterCategory(WritePayloadType.WIFI, "Wi-Fi", Icons.Default.Wifi),
    WriterCategory(WritePayloadType.VCARD, "vCard Contact", Icons.Default.ContactPhone),
    WriterCategory(WritePayloadType.PHONE, "Phone", Icons.Default.Phone),
    WriterCategory(WritePayloadType.SMS, "SMS", Icons.Default.Sms),
    WriterCategory(WritePayloadType.APPLICATION_RECORD, "App AAR", Icons.Default.Android),
    WriterCategory(WritePayloadType.FORMAT_TAG, "Format / Erase", Icons.Default.Delete),
    WriterCategory(WritePayloadType.LOCK_TAG, "Lock Tag", Icons.Default.Lock)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagWriterScreen(
    currentPayload: WritePayload,
    writeState: WriteState,
    onPayloadChange: (WritePayload) -> Unit,
    onStartWrite: (WritePayload) -> Unit,
    onCancelWrite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(currentPayload.type) }

    // Text inputs
    var textValue by remember { mutableStateOf(currentPayload.text.ifBlank { "Hello from NFC Manager!" }) }
    var uriValue by remember { mutableStateOf(currentPayload.uri.ifBlank { "https://github.com" }) }
    var wifiSsid by remember { mutableStateOf(currentPayload.wifiSsid.ifBlank { "Home-WiFi" }) }
    var wifiPassword by remember { mutableStateOf(currentPayload.wifiPassword.ifBlank { "password123" }) }
    var wifiAuthType by remember { mutableStateOf(currentPayload.wifiAuthType) }
    var vcardName by remember { mutableStateOf(currentPayload.vcardName.ifBlank { "John Doe" }) }
    var vcardPhone by remember { mutableStateOf(currentPayload.vcardPhone.ifBlank { "+84901234567" }) }
    var vcardEmail by remember { mutableStateOf(currentPayload.vcardEmail.ifBlank { "john@example.com" }) }
    var vcardOrg by remember { mutableStateOf(currentPayload.vcardOrg.ifBlank { "Acme Corp" }) }
    var phoneValue by remember { mutableStateOf(currentPayload.phoneNumber.ifBlank { "+84901234567" }) }
    var smsNumber by remember { mutableStateOf(currentPayload.smsNumber.ifBlank { "+84901234567" }) }
    var smsBody by remember { mutableStateOf(currentPayload.smsBody.ifBlank { "Hello!" }) }
    var packageName by remember { mutableStateOf(currentPayload.packageName.ifBlank { "com.google.android.youtube" }) }
    var lockReadOnly by remember { mutableStateOf(currentPayload.lockTagReadOnly) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "NFC Tag Writer & Formatter",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )

        // Type Selector Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(WRITER_CATEGORIES) { cat ->
                val isSelected = selectedType == cat.type
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable {
                        selectedType = cat.type
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Form Fields based on selected category
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedType) {
                    WritePayloadType.TEXT -> {
                        Text("Write Plain Text Message", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Text content") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }

                    WritePayloadType.URI -> {
                        Text("Write Website URL or URI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uriValue,
                            onValueChange = { uriValue = it },
                            label = { Text("Website URL (e.g. https://...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    WritePayloadType.WIFI -> {
                        Text("Write Wi-Fi Access Point Config", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("Wi-Fi SSID Network Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = wifiPassword,
                            onValueChange = { wifiPassword = it },
                            label = { Text("Password (WPA2/WPA3)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Auth type pills
                        Text("Authentication Security:", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("WPA2", "WPA3", "OPEN").forEach { auth ->
                                val isAuthSelected = wifiAuthType.equals(auth, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAuthSelected) IndigoSecondary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable { wifiAuthType = auth }
                                ) {
                                    Text(
                                        text = auth,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAuthSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    WritePayloadType.VCARD -> {
                        Text("Write Electronic Business Card (vCard)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = vcardName, onValueChange = { vcardName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = vcardPhone, onValueChange = { vcardPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = vcardEmail, onValueChange = { vcardEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = vcardOrg, onValueChange = { vcardOrg = it }, label = { Text("Company / Organization") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }

                    WritePayloadType.PHONE -> {
                        Text("Write Direct Phone Dial Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = phoneValue, onValueChange = { phoneValue = it }, label = { Text("Phone Number (e.g. +84...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }

                    WritePayloadType.SMS -> {
                        Text("Write SMS Message Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = smsNumber, onValueChange = { smsNumber = it }, label = { Text("Recipient Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = smsBody, onValueChange = { smsBody = it }, label = { Text("SMS Message Body") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    }

                    WritePayloadType.APPLICATION_RECORD -> {
                        Text("Write Android App Launch Record (AAR)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Package Name (e.g. com.example.app)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }

                    WritePayloadType.FORMAT_TAG -> {
                        Text("Format & Erase Tag", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "This operation will format uninitialized NDEF tags or clear existing records by writing an empty NDEF container.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    WritePayloadType.LOCK_TAG -> {
                        Text("Permanently Lock Tag (Read-Only)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StatusError)
                        Text(
                            text = "WARNING: This permanently sets the tag hardware lock bits. You will NEVER be able to write or overwrite data on this tag again!",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }

                if (selectedType != WritePayloadType.LOCK_TAG && selectedType != WritePayloadType.FORMAT_TAG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = lockReadOnly, onCheckedChange = { lockReadOnly = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("Make tag Read-Only after writing", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("Tag cannot be modified again once locked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Action Button: Write to Tag
        Button(
            onClick = {
                val payload = WritePayload(
                    type = selectedType,
                    text = textValue,
                    uri = uriValue,
                    wifiSsid = wifiSsid,
                    wifiPassword = wifiPassword,
                    wifiAuthType = wifiAuthType,
                    vcardName = vcardName,
                    vcardPhone = vcardPhone,
                    vcardEmail = vcardEmail,
                    vcardOrg = vcardOrg,
                    phoneNumber = phoneValue,
                    smsNumber = smsNumber,
                    smsBody = smsBody,
                    packageName = packageName,
                    lockTagReadOnly = lockReadOnly
                )
                onStartWrite(payload)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedType == WritePayloadType.LOCK_TAG) StatusError else CyanPrimary,
                contentColor = if (selectedType == WritePayloadType.LOCK_TAG) Color.White else Color.Black
            )
        ) {
            Icon(
                imageVector = if (selectedType == WritePayloadType.LOCK_TAG) Icons.Default.Lock else if (selectedType == WritePayloadType.FORMAT_TAG) Icons.Default.Delete else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (selectedType == WritePayloadType.LOCK_TAG) "Lock Tag Permanently" else if (selectedType == WritePayloadType.FORMAT_TAG) "Format / Erase Tag" else "Write to Tag",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Modal Bottom Sheet when waiting for NFC Tag to write
    if (writeState !is WriteState.Idle) {
        ModalBottomSheet(
            onDismissRequest = onCancelWrite,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (writeState) {
                    is WriteState.WaitingForTag -> {
                        RadarPulseScanner(
                            title = "Ready to Write",
                            subtitle = "Hold NFC tag steady against the back of your phone..."
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onCancelWrite,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    is WriteState.Writing -> {
                        CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Writing payload to tag...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Do not move the tag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    is WriteState.Success -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Write Successful!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        Text(writeState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onCancelWrite,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }

                    is WriteState.Error -> {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(38.dp))
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Write Failed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = StatusError)
                        Text(writeState.error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onCancelWrite,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                            }
                            Button(
                                onClick = { onStartWrite(currentPayload) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
