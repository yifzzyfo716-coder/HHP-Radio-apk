package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.RadioViewModel
import com.example.ui.components.MicrophonePermissionExplanationDialog
import com.example.ui.screens.AudioSettingsScreen
import com.example.ui.screens.ChannelListScreen
import com.example.ui.screens.KeySettingsScreen
import com.example.ui.screens.MainRadioScreen
import com.example.ui.screens.ServerConfigScreen
import com.example.ui.screens.SettingsAndLogsScreen
import com.example.ui.theme.HhpRadioTheme
import com.example.ui.theme.RadioCyanHighlight
import com.example.ui.theme.RadioNavyCard
import com.example.ui.theme.RadioNavyDark
import com.example.ui.theme.RadioNavyElevated
import com.example.ui.theme.RadioTextDim
import com.example.ui.theme.RadioTextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    private var showPermissionDialog by mutableStateOf(false)
    private var isPermanentlyDenied by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermission()
        if (!isGranted) {
            val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            )
            isPermanentlyDenied = !shouldShow
        } else {
            isPermanentlyDenied = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HhpRadioTheme {
                HhpRadioMainApp(
                    viewModel = viewModel,
                    onRequestMicPermission = {
                        checkAndRequestPermission()
                    }
                )

                if (showPermissionDialog) {
                    MicrophonePermissionExplanationDialog(
                        isPermanentlyDenied = isPermanentlyDenied,
                        onDismiss = { showPermissionDialog = false },
                        onConfirmRequest = {
                            showPermissionDialog = false
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission when user returns from system settings
        viewModel.refreshPermission()
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            isPermanentlyDenied = false
            showPermissionDialog = false
        }
    }

    private fun checkAndRequestPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.refreshPermission()
            return
        }

        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.RECORD_AUDIO
        )

        // Show our Thai explanation screen first per specification
        showPermissionDialog = true
    }

    // Hardware PTT Key support (Volume Down / Headset Hook)
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val bindings = viewModel.keyBindings.value
        if (bindings.allowVolumeKeyPtt && keyCode == bindings.pttKeyCode) {
            if (event?.repeatCount == 0) {
                if (!bindings.isToggleMode) {
                    viewModel.startPtt()
                } else {
                    viewModel.togglePtt()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val bindings = viewModel.keyBindings.value
        if (bindings.allowVolumeKeyPtt && keyCode == bindings.pttKeyCode) {
            if (!bindings.isToggleMode) {
                viewModel.stopPtt()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

enum class NavDestination {
    RADIO,
    CHANNELS,
    SERVERS,
    AUDIO,
    KEYS,
    SETTINGS
}

@Composable
fun HhpRadioMainApp(
    viewModel: RadioViewModel,
    onRequestMicPermission: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(NavDestination.RADIO) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RadioNavyDark,
        bottomBar = {
            NavigationBar(
                containerColor = RadioNavyCard,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == NavDestination.RADIO,
                    onClick = { currentScreen = NavDestination.RADIO },
                    icon = { Icon(Icons.Default.Radio, contentDescription = "วิทยุ") },
                    label = { Text("วิทยุ", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RadioCyanHighlight,
                        selectedTextColor = RadioCyanHighlight,
                        indicatorColor = RadioNavyElevated,
                        unselectedIconColor = RadioTextDim,
                        unselectedTextColor = RadioTextDim
                    ),
                    modifier = Modifier.testTag("nav_tab_radio")
                )
                NavigationBarItem(
                    selected = currentScreen == NavDestination.CHANNELS,
                    onClick = { currentScreen = NavDestination.CHANNELS },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "ช่อง") },
                    label = { Text("ช่อง", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RadioCyanHighlight,
                        selectedTextColor = RadioCyanHighlight,
                        indicatorColor = RadioNavyElevated,
                        unselectedIconColor = RadioTextDim,
                        unselectedTextColor = RadioTextDim
                    ),
                    modifier = Modifier.testTag("nav_tab_channels")
                )
                NavigationBarItem(
                    selected = currentScreen == NavDestination.SERVERS,
                    onClick = { currentScreen = NavDestination.SERVERS },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "เซิร์ฟเวอร์") },
                    label = { Text("เซิร์ฟเวอร์", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RadioCyanHighlight,
                        selectedTextColor = RadioCyanHighlight,
                        indicatorColor = RadioNavyElevated,
                        unselectedIconColor = RadioTextDim,
                        unselectedTextColor = RadioTextDim
                    ),
                    modifier = Modifier.testTag("nav_tab_servers")
                )
                NavigationBarItem(
                    selected = currentScreen == NavDestination.AUDIO,
                    onClick = { currentScreen = NavDestination.AUDIO },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "ระบบเสียง") },
                    label = { Text("ระบบเสียง", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RadioCyanHighlight,
                        selectedTextColor = RadioCyanHighlight,
                        indicatorColor = RadioNavyElevated,
                        unselectedIconColor = RadioTextDim,
                        unselectedTextColor = RadioTextDim
                    ),
                    modifier = Modifier.testTag("nav_tab_audio")
                )
                NavigationBarItem(
                    selected = currentScreen == NavDestination.SETTINGS,
                    onClick = { currentScreen = NavDestination.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "ตั้งค่า") },
                    label = { Text("ตั้งค่า", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RadioCyanHighlight,
                        selectedTextColor = RadioCyanHighlight,
                        indicatorColor = RadioNavyElevated,
                        unselectedIconColor = RadioTextDim,
                        unselectedTextColor = RadioTextDim
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavDestination.RADIO -> MainRadioScreen(
                    viewModel = viewModel,
                    onRequestMicPermission = onRequestMicPermission,
                    onNavigateToChannels = { currentScreen = NavDestination.CHANNELS },
                    onNavigateToServers = { currentScreen = NavDestination.SERVERS },
                    onNavigateToAudio = { currentScreen = NavDestination.AUDIO },
                    onNavigateToKeys = { currentScreen = NavDestination.KEYS },
                    onNavigateToSettings = { currentScreen = NavDestination.SETTINGS }
                )
                NavDestination.CHANNELS -> ChannelListScreen(
                    viewModel = viewModel,
                    onChannelSelected = {
                        currentScreen = NavDestination.RADIO
                    }
                )
                NavDestination.SERVERS -> ServerConfigScreen(
                    viewModel = viewModel
                )
                NavDestination.AUDIO -> AudioSettingsScreen(
                    viewModel = viewModel,
                    onRequestMicPermission = onRequestMicPermission
                )
                NavDestination.KEYS -> KeySettingsScreen(
                    viewModel = viewModel
                )
                NavDestination.SETTINGS -> SettingsAndLogsScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
