package com.thinhzero.seg

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thinhzero.seg.ui.screens.HomeScreen
import com.thinhzero.seg.ui.screens.ResultScreen
import com.thinhzero.seg.ui.screens.ScanScreen
import com.thinhzero.seg.ui.screens.WriteScreen
import com.thinhzero.seg.ui.theme.SegTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    var scannedTagData by mutableStateOf("")
    var nfcStatus by mutableStateOf("Checking...")
    var isScanning by mutableStateOf(false)
    var currentScanMode by mutableStateOf("read") // "read", "emv", "write"
    var pendingWriteData by mutableStateOf("")
    var pendingWriteType by mutableStateOf("text")
    var hasResult by mutableStateOf(false)
    var navigateToResult by mutableStateOf(false)

    // SELECT PPSE (2PAY.SYS.DDF01)
    private val SELECT_PPSE = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
        0x0E.toByte(),
        0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53,
        0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
        0x00.toByte()
    )

    // Common AIDs to try
    private val KNOWN_AIDS = listOf(
        byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10), // Visa
        byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10), // Mastercard
        byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x65.toByte(), 0x10, 0x10), // JCB
        byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x25, 0x01, 0x01, 0x04), // AMEX
        byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x01, 0x52, 0x30, 0x10), // Discover
    )

    // GET PROCESSING OPTIONS
    private val GPO = byteArrayOf(
        0x80.toByte(), 0xA8.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x02.toByte(), 0x83.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            RustBridge.nativeInit()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val hasNfc = nfcAdapter != null
        val isEnabled = nfcAdapter?.isEnabled == true

        nfcStatus = when {
            !hasNfc -> """{"has_nfc":false,"is_enabled":false,"message":"NFC is not available on this device"}"""
            !isEnabled -> """{"has_nfc":true,"is_enabled":false,"message":"NFC is disabled. Please enable it in Settings"}"""
            else -> """{"has_nfc":true,"is_enabled":true,"message":"NFC is ready"}"""
        }

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        setContent {
            SegTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                nfcStatus = nfcStatus,
                                onScanClick = {
                                    currentScanMode = "read"
                                    isScanning = true
                                    hasResult = false
                                    scannedTagData = ""
                                    navController.navigate("scan/read")
                                },
                                onBankCardClick = {
                                    currentScanMode = "emv"
                                    isScanning = true
                                    hasResult = false
                                    scannedTagData = ""
                                    navController.navigate("scan/emv")
                                },
                                onWriteClick = {
                                    navController.navigate("write")
                                }
                            )
                        }
                        composable("scan/{type}") { backStackEntry ->
                            val type = backStackEntry.arguments?.getString("type") ?: "read"
                            ScanScreen(
                                type = type,
                                scannedData = scannedTagData,
                                hasResult = hasResult,
                                onCancel = {
                                    isScanning = false
                                    navController.popBackStack()
                                },
                                onViewResult = {
                                    navController.navigate("result") {
                                        popUpTo("home")
                                    }
                                }
                            )
                        }
                        composable("result") {
                            ResultScreen(
                                resultData = scannedTagData,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("write") {
                            WriteScreen(
                                onBack = { navController.popBackStack() },
                                onWriteReady = { type, data ->
                                    pendingWriteType = type
                                    pendingWriteData = data
                                    currentScanMode = "write"
                                    isScanning = true
                                    hasResult = false
                                    scannedTagData = ""
                                    navController.navigate("scan/write")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @Suppress("DEPRECATION")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action in listOf(
                NfcAdapter.ACTION_NDEF_DISCOVERED,
                NfcAdapter.ACTION_TAG_DISCOVERED,
                NfcAdapter.ACTION_TECH_DISCOVERED
            )
        ) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                when (currentScanMode) {
                    "write" -> writeToTag(it)
                    "emv" -> readEmvCard(it)
                    else -> readTag(it)
                }
            }
        }
    }

    private fun readTag(tag: Tag) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = tag.id
                val techList = tag.techList.joinToString(",")
                val resultBuilder = StringBuilder()

                // Get ATQA and SAK from NfcA
                var atqaBytes = byteArrayOf(0, 0)
                var sakByte: Byte = 0
                val nfcA = NfcA.get(tag)
                if (nfcA != null) {
                    atqaBytes = nfcA.atqa ?: byteArrayOf(0, 0)
                    sakByte = nfcA.sak.toByte()
                }

                // Analyze tag via Rust
                try {
                    val analysis = RustBridge.analyzeTag(uid, techList, atqaBytes, sakByte)
                    resultBuilder.append("=== TAG INFO ===\n$analysis\n\n")
                } catch (e: Exception) {
                    resultBuilder.append("=== TAG INFO ===\n")
                    resultBuilder.append("UID: ${bytesToHex(uid)}\n")
                    resultBuilder.append("Tech: $techList\n\n")
                }

                // Read NDEF data
                val ndef = Ndef.get(tag)
                if (ndef != null) {
                    try {
                        ndef.connect()
                        val ndefMessage = ndef.ndefMessage
                        if (ndefMessage != null) {
                            try {
                                val parsed = RustBridge.parseNdefMessage(ndefMessage.toByteArray())
                                resultBuilder.append("=== NDEF DATA ===\n$parsed\n\n")
                            } catch (e: Exception) {
                                resultBuilder.append("=== NDEF DATA ===\nRead OK but parse failed: ${e.message}\n\n")
                            }
                        } else {
                            resultBuilder.append("=== NDEF DATA ===\nNo NDEF message found\n\n")
                        }
                        resultBuilder.append("Max NDEF size: ${ndef.maxSize} bytes\n")
                        resultBuilder.append("Writable: ${ndef.isWritable}\n")
                        resultBuilder.append("NDEF Type: ${ndef.type}\n")
                        ndef.close()
                    } catch (e: Exception) {
                        resultBuilder.append("=== NDEF ===\nError reading: ${e.message}\n\n")
                    }
                }

                // Read MIFARE Classic
                val mifareClassic = MifareClassic.get(tag)
                if (mifareClassic != null) {
                    try {
                        mifareClassic.connect()
                        resultBuilder.append("=== MIFARE CLASSIC ===\n")
                        resultBuilder.append("Type: ${mifareClassic.type}\n")
                        resultBuilder.append("Size: ${mifareClassic.size} bytes\n")
                        resultBuilder.append("Sectors: ${mifareClassic.sectorCount}\n")
                        resultBuilder.append("Blocks: ${mifareClassic.blockCount}\n")
                        mifareClassic.close()
                    } catch (e: Exception) {
                        resultBuilder.append("MIFARE Classic: Error - ${e.message}\n")
                    }
                }

                // Read MIFARE Ultralight
                val mifareUl = MifareUltralight.get(tag)
                if (mifareUl != null) {
                    try {
                        mifareUl.connect()
                        resultBuilder.append("=== MIFARE ULTRALIGHT ===\n")
                        resultBuilder.append("Type: ${mifareUl.type}\n")
                        // Read first few pages
                        for (page in 0..3) {
                            try {
                                val data = mifareUl.readPages(page * 4)
                                resultBuilder.append("Page $page: ${bytesToHex(data)}\n")
                            } catch (_: Exception) {
                                break
                            }
                        }
                        mifareUl.close()
                    } catch (e: Exception) {
                        resultBuilder.append("MIFARE UL: Error - ${e.message}\n")
                    }
                }

                withContext(Dispatchers.Main) {
                    scannedTagData = resultBuilder.toString()
                    hasResult = true
                    isScanning = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    scannedTagData = "Error reading tag: ${e.message}"
                    hasResult = true
                    isScanning = false
                }
            }
        }
    }

    private fun readEmvCard(tag: Tag) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = tag.id
                val resultBuilder = StringBuilder()
                resultBuilder.append("=== EMV CARD ===\n")
                resultBuilder.append("UID: ${bytesToHex(uid)}\n\n")

                val isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    isoDep.connect()
                    isoDep.timeout = 5000

                    val apduResponses = mutableListOf<String>()

                    // Step 1: SELECT PPSE
                    val ppseResponse = isoDep.transceive(SELECT_PPSE)
                    val ppseHex = bytesToHex(ppseResponse)
                    apduResponses.add(ppseHex)
                    resultBuilder.append("PPSE Response: $ppseHex\n\n")

                    // Step 2: Extract AID from PPSE and SELECT it
                    val aid = extractAidFromPpse(ppseResponse) ?: findWorkingAid(isoDep)
                    if (aid != null) {
                        val selectAid = buildSelectAidApdu(aid)
                        val aidResponse = isoDep.transceive(selectAid)
                        val aidHex = bytesToHex(aidResponse)
                        apduResponses.add(aidHex)
                        resultBuilder.append("AID Response: $aidHex\n\n")

                        // Step 3: GET PROCESSING OPTIONS
                        try {
                            val gpoResponse = isoDep.transceive(GPO)
                            val gpoHex = bytesToHex(gpoResponse)
                            apduResponses.add(gpoHex)
                            resultBuilder.append("GPO Response: $gpoHex\n\n")
                        } catch (e: Exception) {
                            resultBuilder.append("GPO: ${e.message}\n\n")
                        }

                        // Step 4: READ RECORD for SFI 1-5, Record 1-5
                        for (sfi in 1..5) {
                            for (record in 1..5) {
                                try {
                                    val readRecord = byteArrayOf(
                                        0x00.toByte(), 0xB2.toByte(),
                                        record.toByte(),
                                        ((sfi shl 3) or 0x04).toByte(),
                                        0x00.toByte()
                                    )
                                    val recordResponse = isoDep.transceive(readRecord)
                                    if (recordResponse.size > 2 &&
                                        recordResponse[recordResponse.size - 2] == 0x90.toByte() &&
                                        recordResponse[recordResponse.size - 1] == 0x00.toByte()
                                    ) {
                                        val recHex = bytesToHex(recordResponse)
                                        apduResponses.add(recHex)
                                        resultBuilder.append("SFI=$sfi REC=$record: $recHex\n")
                                    }
                                } catch (_: Exception) { break }
                            }
                        }
                    } else {
                        resultBuilder.append("No AID found\n")
                    }

                    isoDep.close()

                    // Parse all responses with Rust EMV parser
                    try {
                        val jsonInput = apduResponses.joinToString(
                            prefix = "[\"", postfix = "\"]", separator = "\",\""
                        )
                        val emvResult = RustBridge.parseEmvData(jsonInput)
                        resultBuilder.append("\n=== PARSED EMV ===\n$emvResult\n")
                    } catch (e: Exception) {
                        resultBuilder.append("\nEMV Parse Error: ${e.message}\n")
                    }
                } else {
                    resultBuilder.append("This tag does not support IsoDep (not a contactless card)\n")
                }

                withContext(Dispatchers.Main) {
                    scannedTagData = resultBuilder.toString()
                    hasResult = true
                    isScanning = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    scannedTagData = "Error reading card: ${e.message}"
                    hasResult = true
                    isScanning = false
                }
            }
        }
    }

    private fun writeToTag(tag: Tag) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val recordBytes = when (pendingWriteType.lowercase()) {
                    "uri" -> {
                        try {
                            RustBridge.createNdefUriRecord(pendingWriteData)
                        } catch (_: Exception) {
                            createNdefTextRecordFallback(pendingWriteData)
                        }
                    }
                    else -> {
                        try {
                            RustBridge.createNdefTextRecord(pendingWriteData, "en")
                        } catch (_: Exception) {
                            createNdefTextRecordFallback(pendingWriteData)
                        }
                    }
                }

                val ndefRecord = NdefRecord(
                    if (pendingWriteType.lowercase() == "uri") NdefRecord.TNF_WELL_KNOWN else NdefRecord.TNF_WELL_KNOWN,
                    if (pendingWriteType.lowercase() == "uri") NdefRecord.RTD_URI else NdefRecord.RTD_TEXT,
                    byteArrayOf(),
                    recordBytes
                )
                val ndefMessage = NdefMessage(arrayOf(ndefRecord))

                val ndef = Ndef.get(tag)
                if (ndef != null) {
                    ndef.connect()
                    if (ndef.isWritable) {
                        ndef.writeNdefMessage(ndefMessage)
                        ndef.close()
                        withContext(Dispatchers.Main) {
                            scannedTagData = "✅ Write successful!\n\nType: ${pendingWriteType}\nData: $pendingWriteData\nSize: ${ndefMessage.toByteArray().size} bytes"
                            hasResult = true
                            isScanning = false
                            Toast.makeText(this@MainActivity, "Write successful!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        ndef.close()
                        withContext(Dispatchers.Main) {
                            scannedTagData = "❌ Tag is read-only"
                            hasResult = true
                            isScanning = false
                        }
                    }
                } else {
                    // Try NdefFormatable
                    val formatable = NdefFormatable.get(tag)
                    if (formatable != null) {
                        formatable.connect()
                        formatable.format(ndefMessage)
                        formatable.close()
                        withContext(Dispatchers.Main) {
                            scannedTagData = "✅ Tag formatted and written!\n\nType: ${pendingWriteType}\nData: $pendingWriteData"
                            hasResult = true
                            isScanning = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            scannedTagData = "❌ Tag doesn't support NDEF writing"
                            hasResult = true
                            isScanning = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    scannedTagData = "❌ Write failed: ${e.message}"
                    hasResult = true
                    isScanning = false
                }
            }
        }
    }

    // Extract AID from PPSE TLV response
    private fun extractAidFromPpse(response: ByteArray): ByteArray? {
        // Look for tag 4F (AID) in the response
        var i = 0
        while (i < response.size - 2) {
            if (response[i] == 0x4F.toByte() && i + 1 < response.size) {
                val len = response[i + 1].toInt() and 0xFF
                if (i + 2 + len <= response.size) {
                    return response.copyOfRange(i + 2, i + 2 + len)
                }
            }
            i++
        }
        return null
    }

    // Try known AIDs until one works
    private fun findWorkingAid(isoDep: IsoDep): ByteArray? {
        for (aid in KNOWN_AIDS) {
            try {
                val selectApdu = buildSelectAidApdu(aid)
                val response = isoDep.transceive(selectApdu)
                if (response.size >= 2 &&
                    response[response.size - 2] == 0x90.toByte() &&
                    response[response.size - 1] == 0x00.toByte()
                ) {
                    return aid
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        val apdu = ByteArray(6 + aid.size)
        apdu[0] = 0x00 // CLA
        apdu[1] = 0xA4.toByte() // INS: SELECT
        apdu[2] = 0x04 // P1: Select by name
        apdu[3] = 0x00 // P2
        apdu[4] = aid.size.toByte() // Lc
        System.arraycopy(aid, 0, apdu, 5, aid.size)
        apdu[5 + aid.size] = 0x00 // Le
        return apdu
    }

    private fun createNdefTextRecordFallback(text: String): ByteArray {
        val langBytes = "en".toByteArray()
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
        return payload
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt()
            result.append(hexChars[i shr 4 and 0x0F])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }
}