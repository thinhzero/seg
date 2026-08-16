package com.example.data.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import com.example.data.model.ApduLogEntry
import com.example.data.nfc.EmvCardParser.hexToBytes
import com.example.data.nfc.EmvCardParser.toHexString

data class ApduPreset(
    val name: String,
    val description: String,
    val commandHex: String
)

object ApduHelper {

    val PRESETS = listOf(
        ApduPreset(
            name = "Select PPSE (Payment System)",
            description = "Select Contactless Payment System Environment (2PAY.SYS.DDF01)",
            commandHex = "00A404000E325041592E5359532E444446303100"
        ),
        ApduPreset(
            name = "Select PSE (Contact Payment)",
            description = "Select Contact Payment Environment (1PAY.SYS.DDF01)",
            commandHex = "00A404000E315041592E5359532E444446303100"
        ),
        ApduPreset(
            name = "Select Visa AID",
            description = "Select Visa Credit / Debit application AID (A0000000031010)",
            commandHex = "00A4040007A000000003101000"
        ),
        ApduPreset(
            name = "Select Mastercard AID",
            description = "Select Mastercard credit/debit application AID (A0000000041010)",
            commandHex = "00A4040007A000000004101000"
        ),
        ApduPreset(
            name = "Select NAPAS AID",
            description = "Select Vietnam NAPAS national chip card AID (A0000007270001)",
            commandHex = "00A4040007A000000727000100"
        ),
        ApduPreset(
            name = "Select JCB AID",
            description = "Select JCB contactless card AID (A0000000651010)",
            commandHex = "00A4040007A00000065101000"
        ),
        ApduPreset(
            name = "Select Amex AID",
            description = "Select American Express AID (A00000002501)",
            commandHex = "00A4040006A0000000250100"
        ),
        ApduPreset(
            name = "Get Processing Options (GPO)",
            description = "Initiate EMV transaction processing options",
            commandHex = "80A8000002830000"
        ),
        ApduPreset(
            name = "Read Record 1 (SFI 1)",
            description = "Read record #1 from SFI 1",
            commandHex = "00B2010C00"
        ),
        ApduPreset(
            name = "Read Record 2 (SFI 1)",
            description = "Read record #2 from SFI 1",
            commandHex = "00B2020C00"
        ),
        ApduPreset(
            name = "Read Record 1 (SFI 2)",
            description = "Read record #1 from SFI 2",
            commandHex = "00B2011400"
        ),
        ApduPreset(
            name = "Get Challenge (8 bytes)",
            description = "Request 8 bytes random challenge from ICC",
            commandHex = "0084000008"
        )
    )

    private val STATUS_WORD_MAP = mapOf(
        "9000" to "Success / Normal processing (SW_OK)",
        "6100" to "More data available (SW_BYTES_REMAINING_00)",
        "6200" to "No information given (State of non-volatile memory unchanged)",
        "6281" to "Part of returned data may be corrupted",
        "6282" to "End of file or record reached before reading requested number of bytes",
        "6283" to "Selected file invalidated",
        "6284" to "FCI not formatted according to ISO 7816-4",
        "6300" to "Authentication failed",
        "6400" to "State of non-volatile memory unchanged (execution error)",
        "6581" to "Memory failure",
        "6700" to "Wrong length in Lc/Le (SW_WRONG_LENGTH)",
        "6800" to "Functions in CLA not supported",
        "6881" to "Logical channel not supported",
        "6882" to "Secure messaging not supported",
        "6981" to "Command incompatible with file structure",
        "6982" to "Security status not satisfied / PIN required",
        "6983" to "Authentication method blocked",
        "6984" to "Reference data not usable",
        "6985" to "Conditions of use not satisfied",
        "6986" to "Command not allowed (no current EF)",
        "6A80" to "Incorrect parameters in data field",
        "6A81" to "Function not supported",
        "6A82" to "File or application not found (SW_FILE_NOT_FOUND)",
        "6A83" to "Record not found",
        "6A84" to "Not enough memory space in file",
        "6A86" to "Incorrect parameters P1-P2 (SW_WRONG_P1P2)",
        "6A88" to "Referenced data (data object) not found",
        "6B00" to "Wrong parameter(s) P1-P2",
        "6C00" to "Wrong Le length",
        "6D00" to "Instruction code (INS) not supported or invalid",
        "6E00" to "Class (CLA) not supported",
        "6F00" to "No precise diagnosis (Internal error)"
    )

    fun decodeStatusWord(swHex: String): String {
        val clean = swHex.uppercase().replace(" ", "")
        if (clean.length == 4) {
            STATUS_WORD_MAP[clean]?.let { return it }
            if (clean.startsWith("61")) {
                val rem = clean.substring(2).toIntOrNull(16) ?: 0
                return "Command OK, $rem bytes available to fetch with GET RESPONSE"
            }
            if (clean.startsWith("6C")) {
                val correctLe = clean.substring(2).toIntOrNull(16) ?: 0
                return "Wrong Le length, reissue with Le = 0x${clean.substring(2)} ($correctLe bytes)"
            }
            if (clean.startsWith("63C")) {
                val retries = clean.substring(3).toIntOrNull(16) ?: 0
                return "Verification failed, $retries retries remaining"
            }
        }
        return "Unknown Status Word (0x$clean)"
    }

    fun executeApdu(tag: Tag, commandHex: String): ApduLogEntry {
        val cleanHex = commandHex.replace(" ", "").replace(":", "")
        val startTime = System.currentTimeMillis()
        val cmdBytes = try {
            cleanHex.hexToBytes()
        } catch (e: Exception) {
            return ApduLogEntry(
                commandHex = cleanHex,
                responseHex = "",
                sw1Sw2 = "",
                statusDescription = "Invalid Hex Command: ${e.message}",
                isSuccess = false,
                durationMs = 0
            )
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            return try {
                if (!isoDep.isConnected) isoDep.connect()
                val responseBytes = isoDep.transceive(cmdBytes)
                val duration = System.currentTimeMillis() - startTime
                val resHex = responseBytes.toHexString()
                val swHex = if (responseBytes.size >= 2) {
                    "%02X%02X".format(
                        responseBytes[responseBytes.size - 2],
                        responseBytes[responseBytes.size - 1]
                    )
                } else ""
                val isSuccess = swHex == "9000" || swHex.startsWith("61")

                ApduLogEntry(
                    commandHex = cleanHex,
                    responseHex = resHex,
                    sw1Sw2 = swHex,
                    statusDescription = decodeStatusWord(swHex),
                    isSuccess = isSuccess,
                    durationMs = duration
                )
            } catch (e: Exception) {
                ApduLogEntry(
                    commandHex = cleanHex,
                    responseHex = "",
                    sw1Sw2 = "",
                    statusDescription = "Transceive Error: ${e.message}",
                    isSuccess = false,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            return try {
                if (!nfcA.isConnected) nfcA.connect()
                val responseBytes = nfcA.transceive(cmdBytes)
                val duration = System.currentTimeMillis() - startTime
                val resHex = responseBytes.toHexString()

                ApduLogEntry(
                    commandHex = cleanHex,
                    responseHex = resHex,
                    sw1Sw2 = "",
                    statusDescription = "NFC-A Raw Transceive Success (${responseBytes.size} bytes)",
                    isSuccess = true,
                    durationMs = duration
                )
            } catch (e: Exception) {
                ApduLogEntry(
                    commandHex = cleanHex,
                    responseHex = "",
                    sw1Sw2 = "",
                    statusDescription = "NFC-A Transceive Error: ${e.message}",
                    isSuccess = false,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }

        return ApduLogEntry(
            commandHex = cleanHex,
            responseHex = "",
            sw1Sw2 = "",
            statusDescription = "Tag does not support IsoDep or NfcA transceive!",
            isSuccess = false,
            durationMs = 0
        )
    }
}
