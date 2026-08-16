package com.thinhzero.seg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    onBack: () -> Unit,
    onWriteReady: (String, String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableIntStateOf(0) } // 0 = Text, 1 = URI
    val types = listOf("Text", "URI")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Tag") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Record Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        onClick = { selectedType = index },
                        selected = selectedType == index
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = {
                    Text(
                        if (selectedType == 1) "URI (e.g. https://example.com)"
                        else "Text to write"
                    )
                },
                placeholder = {
                    Text(
                        if (selectedType == 1) "https://"
                        else "Enter your message..."
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview card
            if (textInput.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Type: ${types[selectedType]}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Data: $textInput",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Size: ~${textInput.toByteArray().size + 7} bytes",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onWriteReady(types[selectedType], textInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = textInput.isNotEmpty(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Proceed to Write", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}