package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmvCardDetails
import com.example.ui.components.CreditCardView
import com.example.ui.components.EmvTagTreeViewer
import com.example.ui.components.RadarPulseScanner
import com.example.ui.components.copyToClipboard

@Composable
fun EmvReaderScreen(
    card: EmvCardDetails?,
    isMasked: Boolean,
    onToggleMask: () -> Unit,
    onClearScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (card == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RadarPulseScanner(
                title = "Waiting for EMV Contactless Card",
                subtitle = "Touch any contactless Visa, Mastercard, NAPAS, JCB, or Amex card to the back of your phone to read card info"
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Realistic Card View
            item {
                CreditCardView(
                    card = card,
                    isMasked = isMasked,
                    onToggleMask = onToggleMask
                )
            }

            // Quick Actions & Re-scan
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card Profile Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = onClearScan,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan New Card", fontSize = 12.sp)
                    }
                }
            }

            // Info Grid Cards
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(
                            icon = Icons.Default.CreditCard,
                            label = "Card Scheme",
                            value = card.cardScheme.displayName
                        )
                        InfoRow(
                            icon = Icons.Default.Tag,
                            label = "Application ID (AID)",
                            value = card.aid.ifBlank { "N/A" },
                            isMonospace = true,
                            onCopy = { copyToClipboard(context, "AID", card.aid) }
                        )
                        InfoRow(
                            icon = Icons.Default.Info,
                            label = "Application Label",
                            value = card.applicationLabel.ifBlank { "N/A" }
                        )
                        if (card.issuerCountryCode.isNotEmpty()) {
                            InfoRow(
                                icon = Icons.Default.Public,
                                label = "Issuer Country Code",
                                value = "ISO ${card.issuerCountryCode}"
                            )
                        }
                        if (card.serviceCode.isNotEmpty()) {
                            InfoRow(
                                icon = Icons.Default.Security,
                                label = "Service Code",
                                value = card.serviceCode,
                                isMonospace = true
                            )
                        }
                        if (card.atrHts.isNotEmpty()) {
                            InfoRow(
                                icon = Icons.Default.Language,
                                label = "Historical Bytes / ATR",
                                value = card.atrHts,
                                isMonospace = true,
                                onCopy = { copyToClipboard(context, "ATR", card.atrHts) }
                            )
                        }
                    }
                }
            }

            // Parsed EMV Tags Tree Viewer
            item {
                EmvTagTreeViewer(
                    tags = card.tagList,
                    rawApdus = card.rawApdus
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                )
            }
        }

        if (onCopy != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "COPY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onCopy() }
                )
            }
        }
    }
}
