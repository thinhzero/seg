package com.example.data.model

enum class NdefRecordType {
    TEXT,
    URI,
    WIFI,
    VCARD,
    AAR,
    PHONE,
    SMS,
    MIME,
    UNKNOWN
}

data class ParsedNdefRecord(
    val type: NdefRecordType,
    val title: String,
    val content: String,
    val rawBytesHex: String = "",
    val details: Map<String, String> = emptyMap()
)

data class NfcTagDetails(
    val idHex: String,
    val idDec: String,
    val techList: List<String>,
    val tagType: String,
    val maxTransceiveLength: Int = 0,
    val isNdefSupported: Boolean = false,
    val isWritable: Boolean = false,
    val isCanMakeReadOnly: Boolean = false,
    val ndefMaxSize: Int = 0,
    val ndefCurrentSize: Int = 0,
    val records: List<ParsedNdefRecord> = emptyList(),
    val historicalBytesHex: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class WritePayloadType {
    TEXT,
    URI,
    WIFI,
    VCARD,
    PHONE,
    SMS,
    APPLICATION_RECORD,
    FORMAT_TAG,
    LOCK_TAG
}

data class WritePayload(
    val type: WritePayloadType,
    val text: String = "",
    val uri: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiAuthType: String = "WPA2", // OPEN, WEP, WPA, WPA2, WPA3
    val vcardName: String = "",
    val vcardPhone: String = "",
    val vcardEmail: String = "",
    val vcardOrg: String = "",
    val phoneNumber: String = "",
    val smsNumber: String = "",
    val smsBody: String = "",
    val packageName: String = "",
    val lockTagReadOnly: Boolean = false
)

sealed class WriteState {
    object Idle : WriteState()
    data class WaitingForTag(val payload: WritePayload) : WriteState()
    object Writing : WriteState()
    data class Success(val message: String) : WriteState()
    data class Error(val error: String) : WriteState()
}

data class ApduLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val commandHex: String,
    val responseHex: String,
    val sw1Sw2: String,
    val statusDescription: String,
    val isSuccess: Boolean,
    val durationMs: Long
)

data class NfcHardwareInfo(
    val hasNfc: Boolean,
    val isEnabled: Boolean,
    val hasHce: Boolean,
    val hasBeam: Boolean,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val deviceModel: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    val supportedTechSummary: List<String> = emptyList()
)
