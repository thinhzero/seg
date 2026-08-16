package com.example.data.nfc

import android.nfc.tech.IsoDep
import com.example.data.model.CardScheme
import com.example.data.model.EmvCardDetails
import com.example.data.model.EmvTagEntry
import java.io.ByteArrayOutputStream

object EmvCardParser {

    private val PPSE_AID = "325041592E5359532E4444463031".hexToBytes() // 2PAY.SYS.DDF01
    private val PSE_AID = "315041592E5359532E4444463031".hexToBytes()  // 1PAY.SYS.DDF01

    private val KNOWN_AIDS = listOf(
        "A0000000031010", // Visa Credit/Debit
        "A0000000032010", // Visa Electron
        "A0000000041010", // Mastercard
        "A0000000043060", // Maestro
        "A0000007270001", // NAPAS (Vietnam National Payment)
        "A0000000651010", // JCB
        "A00000002501",   // American Express
        "A000000333010101", // UnionPay
        "A0000001523010"  // Discover
    )

    private val EMV_TAG_DICTIONARY = mapOf(
        "4F" to "Application Identifier (AID)",
        "50" to "Application Label",
        "57" to "Track 2 Equivalent Data",
        "5A" to "Application Primary Account Number (PAN)",
        "5F20" to "Cardholder Name",
        "5F24" to "Application Expiration Date",
        "5F25" to "Application Effective Date",
        "5F28" to "Issuer Country Code",
        "5F2D" to "Language Preference",
        "5F30" to "Service Code",
        "5F34" to "Application Primary Account Number (PAN) Sequence Number",
        "82" to "Application Interchange Profile (AIP)",
        "84" to "Dedicated File (DF) Name",
        "87" to "Application Priority Indicator",
        "8C" to "Card Risk Management Data Object List 1 (CDOL1)",
        "8D" to "Card Risk Management Data Object List 2 (CDOL2)",
        "8E" to "Cardholder Verification Method (CVM) List",
        "94" to "Application File Locator (AFL)",
        "9F08" to "Card Application Version Number",
        "9F0D" to "Issuer Action Code - Default",
        "9F0E" to "Issuer Action Code - Denial",
        "9F0F" to "Issuer Action Code - Online",
        "9F11" to "Issuer Code Table Index",
        "9F12" to "Application Preferred Name",
        "9F19" to "Deleted Card Application Version Number",
        "9F26" to "Application Cryptogram (AC)",
        "9F36" to "Application Transaction Counter (ATC)",
        "9F38" to "Processing Options Data Object List (PDOL)",
        "9F42" to "Application Currency Code",
        "9F4D" to "Log Entry",
        "9F6C" to "Card Transaction Qualifiers (CTQ)",
        "A5" to "FCI Proprietary Template",
        "6F" to "FCI Template",
        "77" to "Response Message Template Format 2",
        "80" to "Response Message Template Format 1"
    )

    fun readEmvCard(isoDep: IsoDep): EmvCardDetails {
        if (!isoDep.isConnected) {
            isoDep.connect()
        }
        isoDep.timeout = 5000

        val rawApdus = mutableListOf<String>()
        val parsedTags = mutableMapOf<String, ByteArray>()

        var selectedAid: String? = null
        var atrHts = ""
        try {
            val hist = isoDep.historicalBytes ?: isoDep.hiLayerResponse
            if (hist != null && hist.isNotEmpty()) {
                atrHts = hist.toHexString()
            }
        } catch (_: Exception) {}

        // Step 1: Select PPSE (2PAY.SYS.DDF01)
        val ppseCommand = createSelectApdu(PPSE_AID)
        rawApdus.add(">> SELECT PPSE: ${ppseCommand.toHexString()}")
        val ppseResponse = isoDep.transceive(ppseCommand)
        rawApdus.add("<< ${ppseResponse.toHexString()}")

        var aidsFound = mutableListOf<String>()
        if (isResponseSuccess(ppseResponse)) {
            parseTlv(ppseResponse, 0, ppseResponse.size - 2, parsedTags)
            // Extract AID from Tag 4F if present
            parsedTags["4F"]?.let { aidsFound.add(it.toHexString()) }
        }

        if (aidsFound.isEmpty()) {
            // Try PSE
            val pseCommand = createSelectApdu(PSE_AID)
            rawApdus.add(">> SELECT PSE: ${pseCommand.toHexString()}")
            val pseResponse = isoDep.transceive(pseCommand)
            rawApdus.add("<< ${pseResponse.toHexString()}")
            if (isResponseSuccess(pseResponse)) {
                parseTlv(pseResponse, 0, pseResponse.size - 2, parsedTags)
                parsedTags["4F"]?.let { aidsFound.add(it.toHexString()) }
            }
        }

        if (aidsFound.isEmpty()) {
            // Try known AIDs directly
            for (knownAid in KNOWN_AIDS) {
                val selectApdu = createSelectApdu(knownAid.hexToBytes())
                val res = isoDep.transceive(selectApdu)
                if (isResponseSuccess(res)) {
                    aidsFound.add(knownAid)
                    parseTlv(res, 0, res.size - 2, parsedTags)
                    break
                }
            }
        }

        // Step 2: Select the Application AID
        val targetAidHex = aidsFound.firstOrNull() ?: KNOWN_AIDS.first()
        selectedAid = targetAidHex
        val aidBytes = targetAidHex.hexToBytes()
        val selectAppCommand = createSelectApdu(aidBytes)
        rawApdus.add(">> SELECT AID ($targetAidHex): ${selectAppCommand.toHexString()}")
        val selectAppResponse = isoDep.transceive(selectAppCommand)
        rawApdus.add("<< ${selectAppResponse.toHexString()}")

        if (isResponseSuccess(selectAppResponse)) {
            parseTlv(selectAppResponse, 0, selectAppResponse.size - 2, parsedTags)
        }

        // Step 3: Get Processing Options (GPO)
        val pdolBytes = parsedTags["9F38"]
        val gpoApdu = buildGpoApdu(pdolBytes)
        rawApdus.add(">> GPO: ${gpoApdu.toHexString()}")
        try {
            val gpoResponse = isoDep.transceive(gpoApdu)
            rawApdus.add("<< ${gpoResponse.toHexString()}")
            if (isResponseSuccess(gpoResponse)) {
                parseTlv(gpoResponse, 0, gpoResponse.size - 2, parsedTags)
            }
        } catch (e: Exception) {
            rawApdus.add("<< GPO error: ${e.message}")
        }

        // Step 4: Read Records (Scan SFI 1 to 10, records 1 to 16)
        val aflBytes = parsedTags["94"]
        if (aflBytes != null && aflBytes.isNotEmpty()) {
            readRecordsFromAfl(isoDep, aflBytes, parsedTags, rawApdus)
        } else {
            // Fallback: Read standard SFI records
            for (sfi in 1..4) {
                for (rec in 1..10) {
                    val readRecordApdu = byteArrayOf(
                        0x00.toByte(),
                        0xB2.toByte(),
                        rec.toByte(),
                        ((sfi shl 3) or 4).toByte(),
                        0x00.toByte()
                    )
                    try {
                        val res = isoDep.transceive(readRecordApdu)
                        if (isResponseSuccess(res)) {
                            rawApdus.add(">> READ SFI $sfi REC $rec: ${readRecordApdu.toHexString()}")
                            rawApdus.add("<< ${res.toHexString()}")
                            parseTlv(res, 0, res.size - 2, parsedTags)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }

        // Step 5: Extract Card Data
        var pan = ""
        var expiryMonth = ""
        var expiryYear = ""
        var serviceCode = ""
        var cardholderName = ""

        // From Tag 5A (PAN)
        parsedTags["5A"]?.let {
            pan = it.toHexString().trimEnd('F', 'f')
        }

        // From Tag 57 (Track 2 Equivalent Data)
        parsedTags["57"]?.let { track2Bytes ->
            val track2Hex = track2Bytes.toHexString().uppercase()
            val separatorIndex = track2Hex.indexOf('D').let { if (it >= 0) it else track2Hex.indexOf('=') }
            if (separatorIndex > 0) {
                if (pan.isEmpty()) {
                    pan = track2Hex.substring(0, separatorIndex)
                }
                if (track2Hex.length >= separatorIndex + 5) {
                    val exp = track2Hex.substring(separatorIndex + 1, separatorIndex + 5)
                    expiryYear = "20" + exp.substring(0, 2)
                    expiryMonth = exp.substring(2, 4)
                }
                if (track2Hex.length >= separatorIndex + 8) {
                    serviceCode = track2Hex.substring(separatorIndex + 5, separatorIndex + 8)
                }
            }
        }

        // From Tag 5F24 (Application Expiration Date - YYMMDD)
        parsedTags["5F24"]?.let { expBytes ->
            val expHex = expBytes.toHexString()
            if (expHex.length >= 4) {
                expiryYear = "20" + expHex.substring(0, 2)
                expiryMonth = expHex.substring(2, 4)
            }
        }

        // From Tag 5F20 / 9F0B (Cardholder Name)
        parsedTags["5F20"]?.let {
            cardholderName = String(it, Charsets.ISO_8859_1).trim()
        } ?: parsedTags["9F0B"]?.let {
            cardholderName = String(it, Charsets.ISO_8859_1).trim()
        }

        // App Label (Tag 50)
        var appLabel = ""
        parsedTags["50"]?.let {
            appLabel = String(it, Charsets.ISO_8859_1).trim()
        }

        // App Preferred Name (Tag 9F12)
        var appPreferredName = ""
        parsedTags["9F12"]?.let {
            appPreferredName = String(it, Charsets.ISO_8859_1).trim()
        }

        // Issuer Country Code (Tag 5F28)
        var issuerCountryCode = ""
        parsedTags["5F28"]?.let {
            issuerCountryCode = it.toHexString()
        }

        val cardScheme = detectCardScheme(pan, selectedAid ?: "", appLabel)
        val maskedPan = formatMaskedPan(pan)

        // Convert parsed tags to UI-friendly list
        val tagList = parsedTags.map { (tag, bytes) ->
            val name = EMV_TAG_DICTIONARY[tag] ?: "Tag $tag"
            val decoded = decodeTagValue(tag, bytes)
            EmvTagEntry(
                tagHex = tag,
                tagName = name,
                length = bytes.size,
                valueHex = bytes.toHexString(),
                valueDecoded = decoded
            )
        }.sortedBy { it.tagHex }

        return EmvCardDetails(
            pan = pan,
            maskedPan = maskedPan,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            cardholderName = cardholderName,
            cardScheme = cardScheme,
            applicationLabel = if (appLabel.isNotBlank()) appLabel else cardScheme.displayName,
            applicationPreferredName = appPreferredName,
            aid = selectedAid ?: (parsedTags["4F"]?.toHexString() ?: ""),
            issuerCountryCode = issuerCountryCode,
            serviceCode = serviceCode,
            atrHts = atrHts,
            tagList = tagList,
            rawApdus = rawApdus
        )
    }

    private fun readRecordsFromAfl(
        isoDep: IsoDep,
        aflBytes: ByteArray,
        parsedTags: MutableMap<String, ByteArray>,
        rawApdus: MutableList<String>
    ) {
        var idx = 0
        while (idx + 3 < aflBytes.size) {
            val sfi = (aflBytes[idx].toInt() and 0xFF) shr 3
            val firstRec = aflBytes[idx + 1].toInt() and 0xFF
            val lastRec = aflBytes[idx + 2].toInt() and 0xFF
            for (rec in firstRec..lastRec) {
                val readRecordApdu = byteArrayOf(
                    0x00.toByte(),
                    0xB2.toByte(),
                    rec.toByte(),
                    ((sfi shl 3) or 4).toByte(),
                    0x00.toByte()
                )
                try {
                    val res = isoDep.transceive(readRecordApdu)
                    if (isResponseSuccess(res)) {
                        rawApdus.add(">> AFL READ SFI $sfi REC $rec: ${readRecordApdu.toHexString()}")
                        rawApdus.add("<< ${res.toHexString()}")
                        parseTlv(res, 0, res.size - 2, parsedTags)
                    }
                } catch (_: Exception) {}
            }
            idx += 4
        }
    }

    fun parseTlv(
        data: ByteArray,
        offset: Int,
        length: Int,
        outTags: MutableMap<String, ByteArray>
    ) {
        var i = offset
        val end = offset + length
        while (i < end) {
            val tagByte1 = data[i].toInt() and 0xFF
            i++
            if (tagByte1 == 0x00 || tagByte1 == 0xFF) continue // Padding bytes

            val tagBytes = ByteArrayOutputStream()
            tagBytes.write(tagByte1)

            // Multi-byte tag (last 5 bits are all 1s: 0x1F)
            if ((tagByte1 and 0x1F) == 0x1F) {
                while (i < end) {
                    val nextByte = data[i].toInt() and 0xFF
                    tagBytes.write(nextByte)
                    i++
                    if ((nextByte and 0x80) == 0) break // MSB 0 indicates last byte of tag
                }
            }

            val tagHex = tagBytes.toByteArray().toHexString().uppercase()

            if (i >= end) break

            // Parse Length
            val lenByte1 = data[i].toInt() and 0xFF
            i++
            var valLength = lenByte1
            if ((lenByte1 and 0x80) != 0) {
                val numLenBytes = lenByte1 and 0x7F
                valLength = 0
                for (b in 0 until numLenBytes) {
                    if (i < end) {
                        valLength = (valLength shl 8) or (data[i].toInt() and 0xFF)
                        i++
                    }
                }
            }

            if (i + valLength > end || valLength < 0) {
                break
            }

            val valBytes = ByteArray(valLength)
            System.arraycopy(data, i, valBytes, 0, valLength)

            // Check if constructed tag (bit 6 is 1: 0x20)
            val isConstructed = (tagByte1 and 0x20) != 0
            if (isConstructed) {
                parseTlv(data, i, valLength, outTags)
            } else {
                outTags[tagHex] = valBytes
            }

            i += valLength
        }
    }

    private fun decodeTagValue(tag: String, bytes: ByteArray): String {
        return when (tag) {
            "50", "9F12", "5F2D" -> try {
                String(bytes, Charsets.ISO_8859_1)
            } catch (_: Exception) {
                bytes.toHexString()
            }
            "5F20", "9F0B" -> try {
                String(bytes, Charsets.ISO_8859_1).trim()
            } catch (_: Exception) {
                bytes.toHexString()
            }
            "5A" -> formatCardNumber(bytes.toHexString().trimEnd('F', 'f'))
            "5F24", "5F25" -> {
                val hex = bytes.toHexString()
                if (hex.length >= 4) "${hex.substring(2, 4)}/${hex.substring(0, 2)}" else hex
            }
            "5F28" -> "Country ISO ${bytes.toHexString()}"
            "9F42" -> "Currency ISO ${bytes.toHexString()}"
            else -> bytes.toHexString()
        }
    }

    fun detectCardScheme(pan: String, aid: String, label: String): CardScheme {
        val upperAid = aid.uppercase()
        val upperLabel = label.uppercase()
        return when {
            pan.startsWith("4") || upperAid.contains("A000000003") || upperLabel.contains("VISA") -> CardScheme.VISA
            pan.startsWith("51") || pan.startsWith("52") || pan.startsWith("53") ||
                    pan.startsWith("54") || pan.startsWith("55") ||
                    (pan.length >= 4 && pan.substring(0, 4).toIntOrNull() in 2221..2720) ||
                    upperAid.contains("A000000004") || upperLabel.contains("MASTERCARD") -> CardScheme.MASTERCARD
            pan.startsWith("9704") || upperAid.contains("A000000727") || upperLabel.contains("NAPAS") -> CardScheme.NAPAS
            pan.startsWith("35") || upperAid.contains("A000000065") || upperLabel.contains("JCB") -> CardScheme.JCB
            pan.startsWith("34") || pan.startsWith("37") || upperAid.contains("A000000025") || upperLabel.contains("AMEX") -> CardScheme.AMERICAN_EXPRESS
            pan.startsWith("6011") || pan.startsWith("65") || upperAid.contains("A000000152") -> CardScheme.DISCOVER
            pan.startsWith("62") || upperAid.contains("A000000333") || upperLabel.contains("UNIONPAY") -> CardScheme.UNIONPAY
            else -> CardScheme.GENERIC_EMV
        }
    }

    fun formatMaskedPan(pan: String): String {
        if (pan.length < 8) return pan
        val first4 = pan.take(4)
        val last4 = pan.takeLast(4)
        val middleMaskCount = pan.length - 8
        val middle = "•".repeat(middleMaskCount).chunked(4).joinToString(" ")
        return "$first4 $middle $last4"
    }

    fun formatCardNumber(pan: String): String {
        return pan.chunked(4).joinToString(" ")
    }

    private fun createSelectApdu(aid: ByteArray): ByteArray {
        val out = ByteArray(6 + aid.size)
        out[0] = 0x00.toByte() // CLA
        out[1] = 0xA4.toByte() // INS (SELECT)
        out[2] = 0x04.toByte() // P1 (Select by DF name / AID)
        out[3] = 0x00.toByte() // P2
        out[4] = aid.size.toByte() // Lc
        System.arraycopy(aid, 0, out, 5, aid.size)
        out[out.size - 1] = 0x00.toByte() // Le
        return out
    }

    private fun buildGpoApdu(pdolBytes: ByteArray?): ByteArray {
        if (pdolBytes == null || pdolBytes.isEmpty()) {
            return byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte(), 0x83.toByte(), 0x00.toByte(), 0x00.toByte())
        }
        // Build PDOL dummy data of requested lengths
        var dummyLen = 0
        var i = 0
        while (i < pdolBytes.size) {
            val tag1 = pdolBytes[i].toInt() and 0xFF
            i++
            if ((tag1 and 0x1F) == 0x1F) {
                while (i < pdolBytes.size && (pdolBytes[i].toInt() and 0x80) != 0) i++
                if (i < pdolBytes.size) i++
            }
            if (i < pdolBytes.size) {
                dummyLen += pdolBytes[i].toInt() and 0xFF
                i++
            }
        }
        val data = ByteArray(2 + dummyLen)
        data[0] = 0x83.toByte()
        data[1] = dummyLen.toByte()

        val apdu = ByteArray(5 + data.size + 1)
        apdu[0] = 0x80.toByte()
        apdu[1] = 0xA8.toByte()
        apdu[2] = 0x00.toByte()
        apdu[3] = 0x00.toByte()
        apdu[4] = data.size.toByte()
        System.arraycopy(data, 0, apdu, 5, data.size)
        apdu[apdu.size - 1] = 0x00.toByte()
        return apdu
    }

    fun isResponseSuccess(res: ByteArray): Boolean {
        if (res.size < 2) return false
        val sw1 = res[res.size - 2].toInt() and 0xFF
        val sw2 = res[res.size - 1].toInt() and 0xFF
        return (sw1 == 0x90 && sw2 == 0x00) || (sw1 == 0x61)
    }

    fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    fun String.hexToBytes(): ByteArray {
        val clean = replace(" ", "").replace(":", "")
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
