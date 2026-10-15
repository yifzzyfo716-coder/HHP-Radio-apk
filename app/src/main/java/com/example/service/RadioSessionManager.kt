package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.HhpRadioApp
import com.example.audio.AudioEngine
import com.example.model.AudioRoute
import com.example.model.AudioSettingsData
import com.example.model.ConnectionStatus
import com.example.model.DiagnosticLog
import com.example.model.KeyBindingSettings
import com.example.model.LogLevel
import com.example.model.PttState
import com.example.model.RadioChannel
import com.example.model.RadioUser
import com.example.model.ServerProfile
import com.example.network.HhpLocalStationServer
import com.example.network.RadioClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioSessionManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    val audioEngine = AudioEngine(context)
    val radioClient = RadioClient()
    val localServer = HhpLocalStationServer()

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _pttState = MutableStateFlow(PttState.READY)
    val pttState: StateFlow<PttState> = _pttState.asStateFlow()

    private val _activeProfile = MutableStateFlow<ServerProfile?>(null)
    val activeProfile: StateFlow<ServerProfile?> = _activeProfile.asStateFlow()

    private val _audioSettings = MutableStateFlow(AudioSettingsData())
    val audioSettings: StateFlow<AudioSettingsData> = _audioSettings.asStateFlow()

    private val _keyBindings = MutableStateFlow(KeyBindingSettings())
    val keyBindings: StateFlow<KeyBindingSettings> = _keyBindings.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val diagnosticLogs: StateFlow<List<DiagnosticLog>> = _diagnosticLogs.asStateFlow()

    private val _isHostingStation = MutableStateFlow(false)
    val isHostingStation: StateFlow<Boolean> = _isHostingStation.asStateFlow()

    init {
        refreshPermission()
        setupAudioAndNetworkWiring()
        log("System", "HHP Radio Engine Initialized")
    }

    fun refreshPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _hasMicPermission.value = granted
        if (!granted && _pttState.value == PttState.TRANSMITTING) {
            stopPttTransmission()
            log("Security", "ไมโครโฟนถูกยกเลิกสิทธิ์ หยุดส่งเสียงทันที", LogLevel.WARN)
        }
    }

    private fun setupAudioAndNetworkWiring() {
        // When audio chunks captured from mic -> send to network
        audioEngine.onAudioPacketCaptured = { buffer, length ->
            if (_pttState.value == PttState.TRANSMITTING) {
                radioClient.sendAudioFrame(buffer, length)
            }
        }

        // When audio packet incoming from network -> play via audio track
        radioClient.onIncomingAudio = { pcmBytes ->
            if (_pttState.value != PttState.TRANSMITTING) {
                _pttState.value = PttState.RECEIVING
                audioEngine.playIncomingAudio(pcmBytes)
            }
        }

        // PTT grant/deny callback
        radioClient.onPttGrantCallback = { granted, reason ->
            if (granted) {
                if (_hasMicPermission.value) {
                    val started = audioEngine.startCapture(true)
                    if (started) {
                        _pttState.value = PttState.TRANSMITTING
                        log("PTT", "ได้รับอนุญาตส่งเสียง (TX เริ่มต้น)")
                        updateForegroundServiceNotification()
                    } else {
                        _pttState.value = PttState.ERROR
                        radioClient.releasePtt(radioClient.currentChannelId.value)
                        log("Audio", "ไม่สามารถเปิดไมโครโฟนได้", LogLevel.ERROR)
                    }
                } else {
                    _pttState.value = PttState.ERROR
                    radioClient.releasePtt(radioClient.currentChannelId.value)
                    log("Permission", "ไม่สามารถส่งเสียงได้: ไม่มีสิทธิ์ไมโครโฟน", LogLevel.WARN)
                }
            } else {
                _pttState.value = PttState.BUSY
                log("PTT", reason ?: "ช่องสัญญาณไม่ว่าง", LogLevel.WARN)
            }
        }

        // Observe active speaker to switch PTT state
        scope.launch {
            radioClient.activeSpeaker.collect { speaker ->
                if (_pttState.value != PttState.TRANSMITTING) {
                    if (speaker != null) {
                        _pttState.value = PttState.RECEIVING
                    } else if (_pttState.value == PttState.RECEIVING) {
                        _pttState.value = PttState.READY
                        audioEngine.resetRxLevel()
                    }
                }
                updateForegroundServiceNotification()
            }
        }

        // Observe connection status
        scope.launch {
            radioClient.connectionStatus.collect { status ->
                when (status) {
                    ConnectionStatus.CONNECTED -> {
                        log("Network", "เชื่อมต่อสำเร็จกับ ${radioClient.serverDisplayName.value}")
                        startForegroundService()
                    }
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                        if (_pttState.value == PttState.TRANSMITTING) {
                            stopPttTransmission()
                        }
                        _pttState.value = PttState.READY
                        stopForegroundService()
                    }
                    else -> {}
                }
            }
        }
    }

    fun startPttTransmission() {
        refreshPermission()
        if (!_hasMicPermission.value) {
            _pttState.value = PttState.ERROR
            log("Security", "ไม่สามารถส่งเสียงได้: ยังไม่ได้รับสิทธิ์ RECORD_AUDIO", LogLevel.WARN)
            return
        }

        if (radioClient.connectionStatus.value != ConnectionStatus.CONNECTED) {
            _pttState.value = PttState.ERROR
            log("PTT", "ยังไม่ได้เชื่อมต่อเซิร์ฟเวอร์", LogLevel.WARN)
            return
        }

        if (_pttState.value == PttState.TRANSMITTING) return

        _pttState.value = PttState.PRESSED
        val currentChannel = radioClient.currentChannelId.value
        log("PTT", "ร้องขอส่งเสียงในช่อง $currentChannel")
        radioClient.requestPtt(currentChannel)
    }

    fun stopPttTransmission() {
        if (_pttState.value == PttState.TRANSMITTING || _pttState.value == PttState.PRESSED) {
            audioEngine.stopCapture()
            val currentChannel = radioClient.currentChannelId.value
            radioClient.releasePtt(currentChannel)
            _pttState.value = PttState.READY
            log("PTT", "หยุดส่งเสียง (TX สิ้นสุด)")
            updateForegroundServiceNotification()
        }
    }

    fun connectServer(profile: ServerProfile) {
        _activeProfile.value = profile
        log("Network", "กำลังเชื่อมต่อไปยัง ${profile.name} (${profile.host}:${profile.port})")

        if (profile.isLocalStationHost) {
            startLocalStationServer(profile.port)
        }

        radioClient.connect(
            host = profile.host,
            port = profile.port,
            username = profile.username,
            callsign = profile.callsign,
            token = profile.authToken
        )
    }

    fun disconnectServer() {
        val name = _activeProfile.value?.name ?: "Server"
        log("Network", "ตัดการเชื่อมต่อจาก $name")
        stopPttTransmission()
        radioClient.disconnect()
        if (_isHostingStation.value) {
            stopLocalStationServer()
        }
        _activeProfile.value = null
        stopForegroundService()
    }

    fun startLocalStationServer(port: Int = 8765): Boolean {
        val started = localServer.start()
        _isHostingStation.value = started
        if (started) {
            val localIp = HhpLocalStationServer.getLocalIpAddress()
            log("Station", "เปิดโฮสต์สถานีวิทยุท้องถิ่นที่ $localIp:$port")
        } else {
            log("Station", "ไม่สามารถเปิดโฮสต์สถานีวิทยุท้องถิ่นได้บนพอร์ต $port", LogLevel.ERROR)
        }
        return started
    }

    fun stopLocalStationServer() {
        localServer.stop()
        _isHostingStation.value = false
        log("Station", "ปิดสถานีวิทยุท้องถิ่นแล้ว")
    }

    fun updateAudioSettings(settings: AudioSettingsData) {
        _audioSettings.value = settings
        audioEngine.updateSettings(settings)
        log("Audio", "อัปเดตการตั้งค่าเสียง: เส้นทาง=${settings.audioRoute}")
    }

    fun updateKeyBindings(bindings: KeyBindingSettings) {
        _keyBindings.value = bindings
        log("Key", "อัปเดตปุ่ม PTT: KeyCode=${bindings.pttKeyCode}, Toggle=${bindings.isToggleMode}")
    }

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = DiagnosticLog(tag = tag, message = message, level = level)
        val current = _diagnosticLogs.value.toMutableList()
        if (current.size > 200) {
            current.removeAt(0)
        }
        current.add(entry)
        _diagnosticLogs.value = current
        when (level) {
            LogLevel.INFO -> Log.i("HhpRadio:$tag", message)
            LogLevel.WARN -> Log.w("HhpRadio:$tag", message)
            LogLevel.ERROR -> Log.e("HhpRadio:$tag", message)
        }
    }

    fun clearLogs() {
        _diagnosticLogs.value = emptyList()
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, HhpRadioService::class.java).apply {
                action = HhpRadioService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("RadioSessionManager", "Error starting foreground service", e)
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, HhpRadioService::class.java).apply {
                action = HhpRadioService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("RadioSessionManager", "Error stopping foreground service", e)
        }
    }

    private fun updateForegroundServiceNotification() {
        try {
            val intent = Intent(context, HhpRadioService::class.java).apply {
                action = HhpRadioService.ACTION_UPDATE
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var INSTANCE: RadioSessionManager? = null

        fun getInstance(context: Context): RadioSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RadioSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
