package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CardScheme
import com.example.data.model.EmvCardDetails

@Composable
fun CreditCardView(
    card: EmvCardDetails,
    isMasked: Boolean,
    onToggleMask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (brush, textColor) = when (card.cardScheme) {
        CardScheme.VISA -> Pair(
            Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
            Color.White
        )
        CardScheme.MASTERCARD -> Pair(
            Brush.linearGradient(listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFFE65100))),
            Color.White
        )
        CardScheme.NAPAS -> Pair(
            Brush.linearGradient(listOf(Color(0xFF003973), Color(0xFFE5E5BE))),
            Color.White
        )
        CardScheme.JCB -> Pair(
            Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF42A5F5))),
            Color.White
        )
        CardScheme.AMERICAN_EXPRESS -> Pair(
            Brush.linearGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298))),
            Color.White
        )
        else -> Pair(
            Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155))),
            Color.White
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // Standard ISO/IEC 7810 ID-1 aspect ratio
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
                .padding(20.dp)
        ) {
            // Background subtle circles
            CanvasBackgroundAccent()

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: App Label / Bank Name & Contactless Symbol
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card.applicationLabel.ifBlank { card.cardScheme.displayName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            letterSpacing = 1.sp
                        )
                        if (card.aid.isNotEmpty()) {
                            Text(
                                text = "AID: ${card.aid.take(14)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Contactless Symbol
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = textColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "CONTACTLESS",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Contactless",
                            tint = textColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Middle Row: Golden EMV Chip & Toggle Eye Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmvChipGraphic()

                    IconButton(
                        onClick = onToggleMask,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMasked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isMasked) "Show PAN" else "Hide PAN",
                            tint = textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Card Number (PAN)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayPan = if (isMasked) {
                        card.maskedPan
                    } else {
                        card.pan.chunked(4).joinToString(" ")
                    }

                    Text(
                        text = displayPan.ifBlank { "•••• •••• •••• ••••" },
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 2.sp,
                        fontSize = if (displayPan.length > 20) 16.sp else 19.sp
                    )

                    IconButton(
                        onClick = {
                            copyToClipboard(context, "Card Number", card.pan)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Card Number",
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Bottom Row: Cardholder Name, Expiry Date & Scheme Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Cardholder Name
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CARDHOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = textColor.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = card.cardholderName.ifBlank { "VALUED CUSTOMER" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    // Expiry
                    if (card.expiryMonth.isNotEmpty() && card.expiryYear.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "EXPIRES",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = textColor.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${card.expiryMonth}/${card.expiryYear.takeLast(2)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Card Scheme Badge
                    CardSchemeBadge(scheme = card.cardScheme)
                }
            }
        }
    }
}

@Composable
fun EmvChipGraphic() {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA000),
                        Color(0xFFFFE082)
                    )
                )
            )
            .padding(2.dp)
    ) {
        // Inner chip lines
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE6A100).copy(alpha = 0.3f))
        )
    }
}

@Composable
fun CardSchemeBadge(scheme: CardScheme) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.95f),
        modifier = Modifier.padding(start = 6.dp)
    ) {
        Text(
            text = scheme.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(scheme.brandColorHex),
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CanvasBackgroundAccent() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = size.width * 0.45f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.03f),
            radius = size.width * 0.6f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
        )
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
