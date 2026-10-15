package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HhpRadioApp
import com.example.data.repository.ServerRepository
import com.example.model.AudioRoute
import com.example.model.AudioSettingsData
import com.example.model.ConnectionStatus
import com.example.model.DiagnosticLog
import com.example.model.KeyBindingSettings
import com.example.model.PttState
import com.example.model.RadioChannel
import com.example.model.RadioUser
import com.example.model.ServerProfile
import com.example.service.RadioSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = RadioSessionManager.getInstance(application)
    private val repository = ServerRepository((application as HhpRadioApp).database.serverProfileDao())

    val pttState: StateFlow<PttState> = sessionManager.pttState
    val connectionStatus: StateFlow<ConnectionStatus> = sessionManager.radioClient.connectionStatus
    val serverDisplayName: StateFlow<String> = sessionManager.radioClient.serverDisplayName
    val lastError: StateFlow<String?> = sessionManager.radioClient.lastError
    val channels: StateFlow<List<RadioChannel>> = sessionManager.radioClient.channels
    val users: StateFlow<List<RadioUser>> = sessionManager.radioClient.users
    val currentChannelId: StateFlow<String> = sessionManager.radioClient.currentChannelId
    val activeSpeaker: StateFlow<String?> = sessionManager.radioClient.activeSpeaker
    val pingMs: StateFlow<Long> = sessionManager.radioClient.pingMs

    val hasMicPermission: StateFlow<Boolean> = sessionManager.hasMicPermission
    val txAudioLevel: StateFlow<Float> = sessionManager.audioEngine.txAudioLevel
    val rxAudioLevel: StateFlow<Float> = sessionManager.audioEngine.rxAudioLevel
    val currentRoute: StateFlow<AudioRoute> = sessionManager.audioEngine.currentRoute
    val audioSettings: StateFlow<AudioSettingsData> = sessionManager.audioSettings
    val keyBindings: StateFlow<KeyBindingSettings> = sessionManager.keyBindings
    val diagnosticLogs: StateFlow<List<DiagnosticLog>> = sessionManager.diagnosticLogs
    val activeProfile: StateFlow<ServerProfile?> = sessionManager.activeProfile
    val isHostingStation: StateFlow<Boolean> = sessionManager.isHostingStation

    val serverProfiles: StateFlow<List<ServerProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTestingMic = MutableStateFlow(false)
    val isTestingMic: StateFlow<Boolean> = _isTestingMic.asStateFlow()

    fun refreshPermission() {
        sessionManager.refreshPermission()
    }

    fun startPtt() {
        sessionManager.startPttTransmission()
    }

    fun stopPtt() {
        sessionManager.stopPttTransmission()
    }

    fun togglePtt() {
        if (pttState.value == PttState.TRANSMITTING) {
            stopPtt()
        } else {
            startPtt()
        }
    }

    fun switchChannel(channelId: String) {
        sessionManager.radioClient.switchChannel(channelId)
    }

    fun connectServer(profile: ServerProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile.copy(lastUsedTimestamp = System.currentTimeMillis()))
            sessionManager.connectServer(profile)
        }
    }

    fun disconnectServer() {
        sessionManager.disconnectServer()
    }

    fun saveServerProfile(profile: ServerProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun deleteServerProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    fun updateAudioSettings(settings: AudioSettingsData) {
        sessionManager.updateAudioSettings(settings)
    }

    fun updateKeyBindings(bindings: KeyBindingSettings) {
        sessionManager.updateKeyBindings(bindings)
    }

    fun toggleHostLocalStation(port: Int = 8765) {
        if (isHostingStation.value) {
            sessionManager.stopLocalStationServer()
        } else {
            sessionManager.startLocalStationServer(port)
        }
    }

    fun startMicTest(): Boolean {
        refreshPermission()
        if (!hasMicPermission.value) return false
        val ok = sessionManager.audioEngine.startCapture(hasPermission = true, isTestMode = true)
        _isTestingMic.value = ok
        return ok
    }

    fun stopMicTest() {
        sessionManager.audioEngine.stopCapture()
        _isTestingMic.value = false
    }

    fun clearLogs() {
        sessionManager.clearLogs()
    }
}
