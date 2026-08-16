package com.example.data.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.data.model.NdefRecordType
import com.example.data.model.NfcTagDetails
import com.example.data.model.ParsedNdefRecord
import java.math.BigInteger
import java.nio.charset.Charset

object NfcTagReaderHelper {

    private val URI_PREFIX_MAP = mapOf(
        0x00 to "",
        0x01 to "http://www.",
        0x02 to "https://www.",
        0x03 to "http://",
        0x04 to "https://",
        0x05 to "tel:",
        0x06 to "mailto:",
        0x07 to "ftp://anonymous:anonymous@",
        0x08 to "ftp://ftp.",
        0x09 to "ftps://",
        0x0A to "sftp://",
        0x0B to "smb://",
        0x0C to "nfs://",
        0x0D to "ftp://",
        0x0E to "dav://",
        0x0F to "news:",
        0x10 to "telnet://",
        0x11 to "imap:",
        0x12 to "rtsp://",
        0x13 to "urn:",
        0x14 to "pop:",
        0x15 to "sip:",
        0x16 to "sips:",
        0x17 to "tftp:",
        0x18 to "btspp://",
        0x19 to "btl2cap://",
        0x1A to "btgoep://",
        0x1B to "tcpobex://",
        0x1C to "irdaobex://",
        0x1D to "file://",
        0x1E to "urn:epc:id:",
        0x1F to "urn:epc:tag:",
        0x20 to "urn:epc:pat:",
        0x21 to "urn:epc:raw:",
        0x22 to "urn:epc:",
        0x23 to "urn:nfc:"
    )

    fun parseTag(tag: Tag): NfcTagDetails {
        val idBytes = tag.id
        val idHex = idBytes.joinToString(":") { "%02X".format(it) }
        val idDec = try {
            BigInteger(1, idBytes).toString()
        } catch (_: Exception) {
            "0"
        }

        val techList = tag.techList.map { it.substringAfterLast(".") }

        var tagType = "Generic NFC Tag"
        var isNdefSupported = false
        var isWritable = false
        var isCanMakeReadOnly = false
        var ndefMaxSize = 0
        var ndefCurrentSize = 0
        var maxTransceiveLength = 0
        val parsedRecords = mutableListOf<ParsedNdefRecord>()
        var historicalBytesHex = ""

        // Try NDEF
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            isNdefSupported = true
            tagType = ndef.type
            isWritable = ndef.isWritable
            isCanMakeReadOnly = ndef.canMakeReadOnly()
            ndefMaxSize = ndef.maxSize

            try {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                if (ndefMessage != null) {
                    ndefCurrentSize = ndefMessage.byteArrayLength
                    parsedRecords.addAll(parseNdefMessage(ndefMessage))
                }
                ndef.close()
            } catch (_: Exception) {
                // If connecting failed or cached ndef
                val cachedMsg = ndef.cachedNdefMessage
                if (cachedMsg != null) {
                    ndefCurrentSize = cachedMsg.byteArrayLength
                    parsedRecords.addAll(parseNdefMessage(cachedMsg))
                }
            }
        }

        // Try ISO-DEP
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            tagType = "ISO 14443-4 (IsoDep)"
            maxTransceiveLength = isoDep.maxTransceiveLength
            val hist = isoDep.historicalBytes ?: isoDep.hiLayerResponse
            if (hist != null && hist.isNotEmpty()) {
                historicalBytesHex = hist.joinToString(" ") { "%02X".format(it) }
            }
        }

        // Try MIFARE Classic
        val mifare = MifareClassic.get(tag)
        if (mifare != null) {
            tagType = when (mifare.type) {
                MifareClassic.TYPE_CLASSIC -> "MIFARE Classic (${mifare.size} bytes, ${mifare.sectorCount} sectors)"
                MifareClassic.TYPE_PLUS -> "MIFARE Plus (${mifare.size} bytes)"
                MifareClassic.TYPE_PRO -> "MIFARE Pro (${mifare.size} bytes)"
                else -> "MIFARE Classic (${mifare.size} bytes)"
            }
            maxTransceiveLength = mifare.maxTransceiveLength
        }

        // Try MIFARE Ultralight
        val ultralight = MifareUltralight.get(tag)
        if (ultralight != null) {
            tagType = when (ultralight.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight (64 bytes)"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C (192 bytes)"
                else -> "MIFARE Ultralight"
            }
            maxTransceiveLength = ultralight.maxTransceiveLength
        }

        // Try NFC-A / B / F / V if still generic
        if (tagType == "Generic NFC Tag") {
            if (NfcA.get(tag) != null) tagType = "NFC-A (ISO 14443-3A)"
            else if (NfcB.get(tag) != null) tagType = "NFC-B (ISO 14443-3B)"
            else if (NfcF.get(tag) != null) tagType = "NFC-F (JIS 6319-4 / FeliCa)"
            else if (NfcV.get(tag) != null) tagType = "NFC-V (ISO 15693 / Vicinity)"
        }

        return NfcTagDetails(
            idHex = idHex,
            idDec = idDec,
            techList = techList,
            tagType = tagType,
            maxTransceiveLength = maxTransceiveLength,
            isNdefSupported = isNdefSupported,
            isWritable = isWritable,
            isCanMakeReadOnly = isCanMakeReadOnly,
            ndefMaxSize = ndefMaxSize,
            ndefCurrentSize = ndefCurrentSize,
            records = parsedRecords,
            historicalBytesHex = historicalBytesHex
        )
    }

    fun parseNdefMessage(message: NdefMessage): List<ParsedNdefRecord> {
        val records = message.records ?: return emptyList()
        return records.mapIndexed { index, record ->
            parseNdefRecord(record, index + 1)
        }
    }

    private fun parseNdefRecord(record: NdefRecord, index: Int): ParsedNdefRecord {
        val payload = record.payload ?: ByteArray(0)
        val rawHex = payload.joinToString(" ") { "%02X".format(it) }

        // Check TNF (Type Name Format)
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    parseTextRecord(payload, rawHex, index)
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    parseUriRecord(payload, rawHex, index)
                } else {
                    ParsedNdefRecord(
                        type = NdefRecordType.UNKNOWN,
                        title = "Well-Known Record #$index",
                        content = String(payload, Charsets.UTF_8),
                        rawBytesHex = rawHex
                    )
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val mimeType = String(record.type, Charsets.US_ASCII)
                if (mimeType.equals("application/vnd.wfa.wsc", ignoreCase = true)) {
                    parseWifiRecord(payload, rawHex, index)
                } else if (mimeType.contains("vcard", ignoreCase = true)) {
                    parseVcardRecord(payload, rawHex, index)
                } else {
                    ParsedNdefRecord(
                        type = NdefRecordType.MIME,
                        title = "MIME ($mimeType) #$index",
                        content = try { String(payload, Charsets.UTF_8) } catch (_: Exception) { rawHex },
                        rawBytesHex = rawHex,
                        details = mapOf("MIME Type" to mimeType)
                    )
                }
            }
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                val extType = String(record.type, Charsets.US_ASCII)
                if (extType.equals("android.com:pkg", ignoreCase = true)) {
                    val pkg = String(payload, Charsets.UTF_8)
                    ParsedNdefRecord(
                        type = NdefRecordType.AAR,
                        title = "Android App Record #$index",
                        content = pkg,
                        rawBytesHex = rawHex,
                        details = mapOf("Package Name" to pkg)
                    )
                } else {
                    ParsedNdefRecord(
                        type = NdefRecordType.UNKNOWN,
                        title = "External Type ($extType) #$index",
                        content = String(payload, Charsets.UTF_8),
                        rawBytesHex = rawHex
                    )
                }
            }
            NdefRecord.TNF_ABSOLUTE_URI -> {
                val uriStr = String(payload, Charsets.UTF_8)
                ParsedNdefRecord(
                    type = NdefRecordType.URI,
                    title = "Absolute URI #$index",
                    content = uriStr,
                    rawBytesHex = rawHex
                )
            }
            else -> {
                ParsedNdefRecord(
                    type = NdefRecordType.UNKNOWN,
                    title = "Raw Record #$index (TNF ${record.tnf})",
                    content = try { String(payload, Charsets.UTF_8) } catch (_: Exception) { rawHex },
                    rawBytesHex = rawHex
                )
            }
        }
    }

    private fun parseTextRecord(payload: ByteArray, rawHex: String, index: Int): ParsedNdefRecord {
        if (payload.isEmpty()) {
            return ParsedNdefRecord(NdefRecordType.TEXT, "Text #$index", "", rawHex)
        }
        val status = payload[0].toInt()
        val isUtf16 = (status and 0x80) != 0
        val langLength = status and 0x3F
        val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8

        val lang = if (payload.size >= 1 + langLength) {
            String(payload, 1, langLength, Charsets.US_ASCII)
        } else "en"

        val text = if (payload.size > 1 + langLength) {
            String(payload, 1 + langLength, payload.size - 1 - langLength, charset)
        } else ""

        return ParsedNdefRecord(
            type = NdefRecordType.TEXT,
            title = "Text Record #$index",
            content = text,
            rawBytesHex = rawHex,
            details = mapOf("Language" to lang, "Encoding" to (if (isUtf16) "UTF-16" else "UTF-8"))
        )
    }

    private fun parseUriRecord(payload: ByteArray, rawHex: String, index: Int): ParsedNdefRecord {
        if (payload.isEmpty()) {
            return ParsedNdefRecord(NdefRecordType.URI, "URI #$index", "", rawHex)
        }
        val prefixCode = payload[0].toInt() and 0xFF
        val prefix = URI_PREFIX_MAP[prefixCode] ?: ""
        val restUri = if (payload.size > 1) {
            String(payload, 1, payload.size - 1, Charsets.UTF_8)
        } else ""
        val fullUri = "$prefix$restUri"

        val type = when {
            fullUri.startsWith("tel:") -> NdefRecordType.PHONE
            fullUri.startsWith("sms:") -> NdefRecordType.SMS
            else -> NdefRecordType.URI
        }

        return ParsedNdefRecord(
            type = type,
            title = if (type == NdefRecordType.PHONE) "Phone Number #$index" else if (type == NdefRecordType.SMS) "SMS Link #$index" else "Website URL #$index",
            content = fullUri,
            rawBytesHex = rawHex,
            details = mapOf("Protocol/Scheme" to (if (prefix.isNotEmpty()) prefix else "None"))
        )
    }

    private fun parseWifiRecord(payload: ByteArray, rawHex: String, index: Int): ParsedNdefRecord {
        var ssid = ""
        var authType = "Unknown"
        var password = ""
        var i = 0
        while (i + 4 <= payload.size) {
            val tag = ((payload[i].toInt() and 0xFF) shl 8) or (payload[i + 1].toInt() and 0xFF)
            val len = ((payload[i + 2].toInt() and 0xFF) shl 8) or (payload[i + 3].toInt() and 0xFF)
            i += 4
            if (i + len > payload.size) break
            when (tag) {
                0x1045 -> ssid = String(payload, i, len, Charsets.UTF_8)
                0x1003 -> {
                    if (len >= 2) {
                        val authCode = ((payload[i].toInt() and 0xFF) shl 8) or (payload[i + 1].toInt() and 0xFF)
                        authType = when (authCode) {
                            0x0001 -> "Open"
                            0x0002 -> "WPA-Personal"
                            0x0004 -> "Shared"
                            0x0008 -> "WPA-Enterprise"
                            0x0010 -> "WPA2-Enterprise"
                            0x0020 -> "WPA2-Personal"
                            else -> "WPA2/WPA3"
                        }
                    }
                }
                0x1027 -> password = String(payload, i, len, Charsets.UTF_8)
            }
            i += len
        }

        val details = mutableMapOf<String, String>()
        if (ssid.isNotEmpty()) details["SSID"] = ssid
        details["Security"] = authType
        if (password.isNotEmpty()) details["Password"] = password

        return ParsedNdefRecord(
            type = NdefRecordType.WIFI,
            title = "Wi-Fi Config #$index",
            content = "Wi-Fi: $ssid ($authType)",
            rawBytesHex = rawHex,
            details = details
        )
    }

    private fun parseVcardRecord(payload: ByteArray, rawHex: String, index: Int): ParsedNdefRecord {
        val vcardString = String(payload, Charsets.UTF_8)
        val details = mutableMapOf<String, String>()
        var name = ""

        vcardString.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:", ignoreCase = true) -> {
                    name = trimmed.substring(3).trim()
                    details["Full Name"] = name
                }
                trimmed.startsWith("TEL", ignoreCase = true) -> {
                    val phone = trimmed.substringAfter(":").trim()
                    details["Phone"] = phone
                }
                trimmed.startsWith("EMAIL", ignoreCase = true) -> {
                    val email = trimmed.substringAfter(":").trim()
                    details["Email"] = email
                }
                trimmed.startsWith("ORG:", ignoreCase = true) -> {
                    val org = trimmed.substring(4).trim()
                    details["Organization"] = org
                }
            }
        }

        return ParsedNdefRecord(
            type = NdefRecordType.VCARD,
            title = "Contact vCard #$index",
            content = if (name.isNotEmpty()) name else "Contact Card",
            rawBytesHex = rawHex,
            details = details
        )
    }
}
