package com.example.data.model

enum class CardScheme(
    val displayName: String,
    val brandColorHex: Long,
    val secondaryColorHex: Long
) {
    VISA("Visa", 0xFF1A1F71, 0xFFF7B600),
    MASTERCARD("Mastercard", 0xFFEB001B, 0xFFF79E1B),
    NAPAS("NAPAS", 0xFF005BAA, 0xFF009FE3),
    JCB("JCB", 0xFF0E4C92, 0xFF0079C1),
    AMERICAN_EXPRESS("American Express", 0xFF006FCF, 0xFF002663),
    DISCOVER("Discover", 0xFFFF6000, 0xFF111111),
    UNIONPAY("UnionPay", 0xFF007B78, 0xFFD81E06),
    GENERIC_EMV("EMV Smart Card", 0xFF1E293B, 0xFF334155)
}

data class EmvTagEntry(
    val tagHex: String,
    val tagName: String,
    val length: Int,
    val valueHex: String,
    val valueDecoded: String,
    val description: String = ""
)

data class EmvCardDetails(
    val pan: String,
    val maskedPan: String,
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cardholderName: String = "",
    val cardScheme: CardScheme = CardScheme.GENERIC_EMV,
    val applicationLabel: String = "",
    val applicationPreferredName: String = "",
    val aid: String = "",
    val issuerCountryCode: String = "",
    val serviceCode: String = "",
    val atrHts: String = "",
    val tagList: List<EmvTagEntry> = emptyList(),
    val rawApdus: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
