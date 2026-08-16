package com.thinhzero.seg

object RustBridge {
    init {
        try {
            System.loadLibrary("seg_nfc")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }
    
    external fun nativeInit()
    external fun parseNdefMessage(rawBytes: ByteArray): String
    external fun parseEmvData(rawApduResponses: String): String
    external fun analyzeTag(uid: ByteArray, techList: String, atqa: ByteArray, sak: Byte): String
    external fun createNdefTextRecord(text: String, locale: String): ByteArray
    external fun createNdefUriRecord(uri: String): ByteArray
    external fun checkNfcCapability(hasNfc: Boolean, isEnabled: Boolean): String
}