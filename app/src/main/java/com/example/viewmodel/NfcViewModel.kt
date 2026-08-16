package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TagEntity
import com.example.data.model.ApduLogEntry
import com.example.data.model.CardScheme
import com.example.data.model.EmvCardDetails
import com.example.data.model.NfcHardwareInfo
import com.example.data.model.NfcTagDetails
import com.example.data.model.WritePayload
import com.example.data.model.WritePayloadType
import com.example.data.model.WriteState
import com.example.data.nfc.ApduHelper
import com.example.data.nfc.ApduPreset
import com.example.data.nfc.EmvCardParser
import com.example.data.nfc.NfcTagReaderHelper
import com.example.data.nfc.NfcTagWriterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab {
    DASHBOARD,
    EMV_READER,
    APDU_CONSOLE,
    HISTORY,
    DIAGNOSIS
}

class NfcViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val tagDao = db.tagDao()

    private val _activeTab = MutableStateFlow(MainTab.DASHBOARD)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _isWriteMode = MutableStateFlow(false)
    val isWriteMode: StateFlow<Boolean> = _isWriteMode.asStateFlow()

    private val _hardwareInfo = MutableStateFlow(checkHardware(application))
    val hardwareInfo: StateFlow<NfcHardwareInfo> = _hardwareInfo.asStateFlow()

    private val _scannedEmvCard = MutableStateFlow<EmvCardDetails?>(null)
    val scannedEmvCard: StateFlow<EmvCardDetails?> = _scannedEmvCard.asStateFlow()

    private val _scannedTag = MutableStateFlow<NfcTagDetails?>(null)
    val scannedTag: StateFlow<NfcTagDetails?> = _scannedTag.asStateFlow()

    private val _isMaskPan = MutableStateFlow(true)
    val isMaskPan: StateFlow<Boolean> = _isMaskPan.asStateFlow()

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // Tag Writer State
    private val _writePayload = MutableStateFlow(WritePayload(type = WritePayloadType.TEXT, text = "Hello from NFC Manager!"))
    val writePayload: StateFlow<WritePayload> = _writePayload.asStateFlow()

    private val _writeState = MutableStateFlow<WriteState>(WriteState.Idle)
    val writeState: StateFlow<WriteState> = _writeState.asStateFlow()

    // APDU Console State
    private val _apduLogs = MutableStateFlow<List<ApduLogEntry>>(emptyList())
    val apduLogs: StateFlow<List<ApduLogEntry>> = _apduLogs.asStateFlow()

    private val _customApduInput = MutableStateFlow("00A404000E325041592E5359532E444446303100")
    val customApduInput: StateFlow<String> = _customApduInput.asStateFlow()

    // History & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val historyList: StateFlow<List<TagEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            tagDao.getAllTagsFlow()
        } else {
            tagDao.searchTagsFlow(query)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleWriteMode(isWrite: Boolean) {
        _isWriteMode.value = isWrite
        if (!isWrite) {
            cancelWriteSession()
        }
    }

    fun setActiveTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun refreshHardwareStatus() {
        _hardwareInfo.value = checkHardware(getApplication())
    }

    fun toggleMaskPan() {
        _isMaskPan.value = !_isMaskPan.value
    }

    fun updateWritePayload(payload: WritePayload) {
        _writePayload.value = payload
    }

    fun startWriteSession(payload: WritePayload) {
        _writePayload.value = payload
        _writeState.value = WriteState.WaitingForTag(payload)
    }

    fun cancelWriteSession() {
        _writeState.value = WriteState.Idle
    }

    fun setCustomApduInput(cmd: String) {
        _customApduInput.value = cmd
    }

    fun clearApduLogs() {
        _apduLogs.value = emptyList()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _lastError.value = null
    }

    fun clearCurrentScan() {
        _scannedEmvCard.value = null
        _scannedTag.value = null
    }

    fun deleteHistoryItem(item: TagEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            tagDao.deleteTag(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            tagDao.clearAll()
        }
    }

    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch {
            vibrateSuccess()

            // 1. Check if we are waiting for a write task
            val currentWriteState = _writeState.value
            if (currentWriteState is WriteState.WaitingForTag) {
                _writeState.value = WriteState.Writing
                val result = withContext(Dispatchers.IO) {
                    NfcTagWriterHelper.writeToTag(tag, currentWriteState.payload)
                }
                result.fold(
                    onSuccess = { msg ->
                        _writeState.value = WriteState.Success(msg)
                        saveWrittenToHistory(tag, currentWriteState.payload, msg)
                    },
                    onFailure = { err ->
                        _writeState.value = WriteState.Error(err.message ?: "Write failed")
                    }
                )
                return@launch
            }

            // 2. Check if user is in APDU Console tab and has sent a command
            if (_activeTab.value == MainTab.APDU_CONSOLE) {
                val cmd = _customApduInput.value
                val logEntry = withContext(Dispatchers.IO) {
                    ApduHelper.executeApdu(tag, cmd)
                }
                _apduLogs.value = listOf(logEntry) + _apduLogs.value
                return@launch
            }

            // 3. Normal Read Flow: Scan standard Tag & EMV simultaneously
            _isReading.value = true
            _lastError.value = null

            try {
                // Parse Standard Tag Details
                val tagDetails = withContext(Dispatchers.IO) {
                    NfcTagReaderHelper.parseTag(tag)
                }
                _scannedTag.value = tagDetails

                // Check if ISO-DEP is supported for EMV scanning
                val isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    val emvResult = withContext(Dispatchers.IO) {
                        try {
                            EmvCardParser.readEmvCard(isoDep)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (emvResult != null && (emvResult.pan.isNotEmpty() || emvResult.cardholderName.isNotEmpty() || emvResult.applicationLabel.isNotEmpty())) {
                        _scannedEmvCard.value = emvResult
                        _activeTab.value = MainTab.EMV_READER
                        saveEmvToHistory(tagDetails, emvResult)
                    } else {
                        _activeTab.value = MainTab.DASHBOARD
                        saveTagToHistory(tagDetails)
                    }
                } else {
                    _activeTab.value = MainTab.DASHBOARD
                    saveTagToHistory(tagDetails)
                }
            } catch (e: Exception) {
                _lastError.value = "Read Tag Error: ${e.message}"
            } finally {
                _isReading.value = false
            }
        }
    }

    private fun saveEmvToHistory(tagDetails: NfcTagDetails, card: EmvCardDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            val title = "${card.cardScheme.displayName} (${card.maskedPan})"
            val subtitle = if (card.expiryMonth.isNotEmpty()) "Exp: ${card.expiryMonth}/${card.expiryYear} • ${card.cardholderName}" else card.cardholderName.ifBlank { "AID: ${card.aid}" }
            val entity = TagEntity(
                category = "EMV_CARD",
                title = title,
                subtitle = subtitle,
                tagIdHex = tagDetails.idHex,
                techList = tagDetails.techList.joinToString(", "),
                payloadJson = "{\"scheme\":\"${card.cardScheme.displayName}\",\"pan\":\"${card.pan}\",\"expiry\":\"${card.expiryMonth}/${card.expiryYear}\",\"name\":\"${card.cardholderName}\",\"aid\":\"${card.aid}\"}"
            )
            tagDao.insertTag(entity)
        }
    }

    private fun saveTagToHistory(tagDetails: NfcTagDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            val recordSummary = tagDetails.records.firstOrNull()?.content ?: "Empty NDEF Tag"
            val entity = TagEntity(
                category = "NDEF_TAG",
                title = "${tagDetails.tagType} (${tagDetails.idHex})",
                subtitle = recordSummary.take(60),
                tagIdHex = tagDetails.idHex,
                techList = tagDetails.techList.joinToString(", "),
                payloadJson = "{\"tech\":\"${tagDetails.techList.joinToString(",")}\",\"recordsCount\":${tagDetails.records.size}}"
            )
            tagDao.insertTag(entity)
        }
    }

    private fun saveWrittenToHistory(tag: Tag, payload: WritePayload, successMsg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tagIdHex = tag.id.joinToString(":") { "%02X".format(it) }
            val title = "Written: ${payload.type.name}"
            val subtitle = when (payload.type) {
                WritePayloadType.TEXT -> payload.text
                WritePayloadType.URI -> payload.uri
                WritePayloadType.WIFI -> "Wi-Fi: ${payload.wifiSsid}"
                WritePayloadType.VCARD -> "Contact: ${payload.vcardName}"
                WritePayloadType.PHONE -> "Phone: ${payload.phoneNumber}"
                WritePayloadType.SMS -> "SMS: ${payload.smsNumber}"
                WritePayloadType.APPLICATION_RECORD -> "App: ${payload.packageName}"
                WritePayloadType.FORMAT_TAG -> "Formatted Tag"
                WritePayloadType.LOCK_TAG -> "Locked as Read-Only"
            }
            val entity = TagEntity(
                category = "WRITTEN_PAYLOAD",
                title = title,
                subtitle = subtitle.take(60),
                tagIdHex = tagIdHex,
                techList = tag.techList.map { it.substringAfterLast(".") }.joinToString(", "),
                payloadJson = "{\"status\":\"$successMsg\"}"
            )
            tagDao.insertTag(entity)
        }
    }

    private fun vibrateSuccess() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    companion object {
        fun checkHardware(context: Context): NfcHardwareInfo {
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
}
