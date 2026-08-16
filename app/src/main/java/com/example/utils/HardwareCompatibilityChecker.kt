package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import com.example.data.model.NfcHardwareInfo

object HardwareCompatibilityChecker {
    fun checkNfcSupport(context: Context): NfcHardwareInfo {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val hasNfc = nfcAdapter != null
        val isEnabled = nfcAdapter?.isEnabled == true
        val hasHce = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
        val hasBeam = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)

        val techSummary = mutableListOf<String>()
        if (hasNfc) {
            techSummary.add("ISO 14443 Type A (NFC-A)")
            techSummary.add("ISO 14443 Type B (NFC-B)")
            techSummary.add("ISO 18092 / FeliCa (NFC-F)")
            techSummary.add("ISO 15693 / Vicinity (NFC-V)")
            techSummary.add("ISO 7816-4 ISO-DEP (Smart / Banking Cards)")
            techSummary.add("NFC Forum Type 1-5 Tags")
            if (hasHce) techSummary.add("Host-based Card Emulation (HCE)")
        }

        return NfcHardwareInfo(
            hasNfc = hasNfc,
            isEnabled = isEnabled,
            hasHce = hasHce,
            hasBeam = hasBeam,
            supportedTechSummary = techSummary
        )
    }
}
