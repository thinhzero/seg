package com.example.data.nfc

import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.example.data.model.WritePayload
import com.example.data.model.WritePayloadType
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object NfcTagWriterHelper {

    fun writeToTag(tag: Tag, payload: WritePayload): Result<String> {
        return try {
            if (payload.type == WritePayloadType.LOCK_TAG) {
                return lockTag(tag)
            }

            val ndefMessage = createNdefMessage(payload)
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(Exception("Tag is write-protected / Read-only!"))
                }
                if (ndefMessage.byteArrayLength > ndef.maxSize) {
                    val needed = ndefMessage.byteArrayLength
                    val max = ndef.maxSize
                    ndef.close()
                    return Result.failure(Exception("Message too large for tag! (Need $needed bytes, tag capacity: $max bytes)"))
                }
                ndef.writeNdefMessage(ndefMessage)

                if (payload.lockTagReadOnly && ndef.canMakeReadOnly()) {
                    ndef.makeReadOnly()
                }

                ndef.close()
                Result.success("Successfully wrote data to NFC tag (${ndefMessage.byteArrayLength} bytes)!")
            } else {
                // Try formatting as NDEF
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    if (payload.lockTagReadOnly) {
                        formatable.formatReadOnly(ndefMessage)
                    } else {
                        formatable.format(ndefMessage)
                    }
                    formatable.close()
                    Result.success("Successfully formatted and wrote NDEF data to tag!")
                } else {
                    Result.failure(Exception("This NFC tag does not support NDEF format!"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun lockTag(tag: Tag): Result<String> {
        val ndef = Ndef.get(tag) ?: return Result.failure(Exception("Tag is not NDEF formatted!"))
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                return Result.failure(Exception("Tag is already locked or read-only."))
            }
            if (!ndef.canMakeReadOnly()) {
                ndef.close()
                return Result.failure(Exception("This tag hardware does not support permanent locking."))
            }
            val success = ndef.makeReadOnly()
            ndef.close()
            if (success) {
                Result.success("Tag has been permanently locked as Read-Only!")
            } else {
                Result.failure(Exception("Failed to lock tag."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createNdefMessage(payload: WritePayload): NdefMessage {
        val record = when (payload.type) {
            WritePayloadType.TEXT -> {
                createTextRecord(payload.text, "en")
            }
            WritePayloadType.URI -> {
                var uri = payload.uri.trim()
                if (!uri.startsWith("http://") && !uri.startsWith("https://") && !uri.startsWith("ftp://")) {
                    uri = "https://$uri"
                }
                NdefRecord.createUri(Uri.parse(uri))
            }
            WritePayloadType.PHONE -> {
                val phone = payload.phoneNumber.trim()
                NdefRecord.createUri("tel:$phone")
            }
            WritePayloadType.SMS -> {
                val num = payload.smsNumber.trim()
                val body = payload.smsBody.trim()
                val uri = if (body.isNotEmpty()) "sms:$num?body=${Uri.encode(body)}" else "sms:$num"
                NdefRecord.createUri(uri)
            }
            WritePayloadType.WIFI -> {
                createWifiRecord(payload.wifiSsid, payload.wifiPassword, payload.wifiAuthType)
            }
            WritePayloadType.VCARD -> {
                createVCardRecord(payload.vcardName, payload.vcardPhone, payload.vcardEmail, payload.vcardOrg)
            }
            WritePayloadType.APPLICATION_RECORD -> {
                NdefRecord.createApplicationRecord(payload.packageName.trim())
            }
            WritePayloadType.FORMAT_TAG -> {
                createTextRecord("", "en")
            }
            WritePayloadType.LOCK_TAG -> {
                createTextRecord("", "en")
            }
        }

        return NdefMessage(arrayOf(record))
    }

    private fun createTextRecord(text: String, languageCode: String): NdefRecord {
        val langBytes = languageCode.toByteArray(Charsets.US_ASCII)
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + langBytes.size + textBytes.size)

        payload[0] = (langBytes.size and 0x3F).toByte() // UTF-8 (bit 7 = 0) + lang length
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)

        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_TEXT,
            ByteArray(0),
            payload
        )
    }

    private fun createWifiRecord(ssid: String, key: String, authType: String): NdefRecord {
        val baos = ByteArrayOutputStream()

        // Credential Container (0x100E)
        val credStream = ByteArrayOutputStream()

        // 1. Network Index (0x1026, 1 byte = 1)
        credStream.write(byteArrayOf(0x10.toByte(), 0x26.toByte(), 0x00.toByte(), 0x01.toByte(), 0x01.toByte()))

        // 2. SSID (0x1045)
        val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
        credStream.write(byteArrayOf(0x10.toByte(), 0x45.toByte(), (ssidBytes.size shr 8).toByte(), (ssidBytes.size and 0xFF).toByte()))
        credStream.write(ssidBytes)

        // 3. Authentication Type (0x1003, 2 bytes)
        val authCode: Short = when (authType.uppercase()) {
            "OPEN" -> 0x0001
            "WPA" -> 0x0002
            "WPA2", "WPA3" -> 0x0020
            else -> 0x0020
        }
        credStream.write(byteArrayOf(0x10.toByte(), 0x03.toByte(), 0x00.toByte(), 0x02.toByte(), (authCode.toInt() shr 8).toByte(), (authCode.toInt() and 0xFF).toByte()))

        // 4. Encryption Type (0x100F, 2 bytes)
        val encCode: Short = if (authType.equals("OPEN", ignoreCase = true)) 0x0001 else 0x0008
        credStream.write(byteArrayOf(0x10.toByte(), 0x0F.toByte(), 0x00.toByte(), 0x02.toByte(), (encCode.toInt() shr 8).toByte(), (encCode.toInt() and 0xFF).toByte()))

        // 5. Network Key (Password) (0x1027)
        if (key.isNotEmpty()) {
            val keyBytes = key.toByteArray(Charsets.UTF_8)
            credStream.write(byteArrayOf(0x10.toByte(), 0x27.toByte(), (keyBytes.size shr 8).toByte(), (keyBytes.size and 0xFF).toByte()))
            credStream.write(keyBytes)
        }

        // 6. MAC Address (0x1020, 6 bytes broadcast)
        credStream.write(byteArrayOf(0x10.toByte(), 0x20.toByte(), 0x00.toByte(), 0x06.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))

        val credBytes = credStream.toByteArray()

        // Wrap into 0x100E container
        baos.write(byteArrayOf(0x10.toByte(), 0x0E.toByte(), (credBytes.size shr 8).toByte(), (credBytes.size and 0xFF).toByte()))
        baos.write(credBytes)

        val mimeType = "application/vnd.wfa.wsc".toByteArray(Charsets.US_ASCII)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeType,
            ByteArray(0),
            baos.toByteArray()
        )
    }

    private fun createVCardRecord(name: String, phone: String, email: String, org: String): NdefRecord {
        val vcard = buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (name.isNotBlank()) appendLine("FN:$name")
            if (name.isNotBlank()) appendLine("N:$name;;;;")
            if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL;TYPE=INTERNET:$email")
            if (org.isNotBlank()) appendLine("ORG:$org")
            appendLine("END:VCARD")
        }

        val mimeType = "text/vcard".toByteArray(Charsets.US_ASCII)
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeType,
            ByteArray(0),
            vcard.toByteArray(Charsets.UTF_8)
        )
    }
}
