package com.example

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NfcStatusBanner
import com.example.ui.components.openNfcSettings
import com.example.ui.screens.ApduTerminalScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmvReaderScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.NfcDiagnosisScreen
import com.example.ui.screens.TagReaderScreen
import com.example.ui.screens.TagWriterScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.viewmodel.MainTab
import com.example.viewmodel.NfcViewModel

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private val viewModel: NfcViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        handleNfcIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHardwareStatus()
        enableNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun enableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        adapter.enableReaderMode(this, this, flags, null)
    }

    private fun disableNfcReaderMode() {
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            viewModel.onTagDiscovered(tag)
        }
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            @Suppress("DEPRECATION")
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                viewModel.onTagDiscovered(tag)
            }
        }
    }
}

data class NavItem(
    val tab: MainTab,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val NAV_ITEMS = listOf(
    NavItem(MainTab.DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavItem(MainTab.EMV_READER, "EMV Card", Icons.Filled.CreditCard, Icons.Outlined.CreditCard),
    NavItem(MainTab.APDU_CONSOLE, "APDU", Icons.Filled.Terminal, Icons.Outlined.Terminal),
    NavItem(MainTab.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: NfcViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isWriteMode by viewModel.isWriteMode.collectAsStateWithLifecycle()
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val emvCard by viewModel.scannedEmvCard.collectAsStateWithLifecycle()
    val tagDetails by viewModel.scannedTag.collectAsStateWithLifecycle()
    val isMaskPan by viewModel.isMaskPan.collectAsStateWithLifecycle()
    val writePayload by viewModel.writePayload.collectAsStateWithLifecycle()
    val writeState by viewModel.writeState.collectAsStateWithLifecycle()
    val apduLogs by viewModel.apduLogs.collectAsStateWithLifecycle()
    val customApduInput by viewModel.customApduInput.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NFC Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Hardware status indicator dot
                        Surface(
                            shape = CircleShape,
                            color = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess.copy(alpha = 0.2f)
                            else if (hardwareInfo.hasNfc) StatusWarning.copy(alpha = 0.2f)
                            else StatusError.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess
                                            else if (hardwareInfo.hasNfc) StatusWarning
                                            else StatusError
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) "ACTIVE"
                                    else if (hardwareInfo.hasNfc) "OFF"
                                    else "NO NFC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hardwareInfo.hasNfc && hardwareInfo.isEnabled) StatusSuccess
                                    else if (hardwareInfo.hasNfc) StatusWarning
                                    else StatusError
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Hardware diagnosis shortcut
                    IconButton(onClick = { viewModel.setActiveTab(MainTab.DIAGNOSIS) }) {
                        Icon(
                            imageVector = if (activeTab == MainTab.DIAGNOSIS) Icons.Filled.MedicalServices else Icons.Outlined.MedicalServices,
                            contentDescription = "Diagnosis",
                            tint = if (activeTab == MainTab.DIAGNOSIS) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // System Settings shortcut
                    IconButton(onClick = { openNfcSettings(context) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NAV_ITEMS.forEach { item ->
                    val selected = activeTab == item.tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.setActiveTab(item.tab) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // NFC status notification banner
            if (activeTab != MainTab.DIAGNOSIS) {
                NfcStatusBanner(
                    hardwareInfo = hardwareInfo,
                    onRefresh = { viewModel.refreshHardwareStatus() }
                )
            }

            // Tab Content with smooth Crossfade
            Crossfade(
                targetState = activeTab,
                label = "tab_transition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            isWriteMode = isWriteMode,
                            onToggleWriteMode = { viewModel.toggleWriteMode(it) },
                            tagDetails = tagDetails,
                            onClearScan = { viewModel.clearCurrentScan() },
                            writePayload = writePayload,
                            writeState = writeState,
                            onPayloadChange = { viewModel.updateWritePayload(it) },
                            onStartWrite = { viewModel.startWriteSession(it) },
                            onCancelWrite = { viewModel.cancelWriteSession() }
                        )
                    }

                    MainTab.EMV_READER -> {
                        EmvReaderScreen(
                            card = emvCard,
                            isMasked = isMaskPan,
                            onToggleMask = { viewModel.toggleMaskPan() },
                            onClearScan = { viewModel.clearCurrentScan() }
                        )
                    }

                    MainTab.APDU_CONSOLE -> {
                        ApduTerminalScreen(
                            apduLogs = apduLogs,
                            currentCommand = customApduInput,
                            onCommandChange = { viewModel.setCustomApduInput(it) },
                            onClearLogs = { viewModel.clearApduLogs() }
                        )
                    }

                    MainTab.HISTORY -> {
                        HistoryScreen(
                            historyList = historyList,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onDeleteItem = { viewModel.deleteHistoryItem(it) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }

                    MainTab.DIAGNOSIS -> {
                        NfcDiagnosisScreen(
                            hardwareInfo = hardwareInfo,
                            onRefresh = { viewModel.refreshHardwareStatus() }
                        )
                    }
                }
            }
        }
    }
}
