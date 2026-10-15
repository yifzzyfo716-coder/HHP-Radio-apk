package com.example.model

data class AudioSettingsData(
    val inputGain: Float = 1.0f,
    val outputVolume: Float = 1.0f,
    val echoCancellation: Boolean = true,
    val noiseSuppression: Boolean = true,
    val autoGainControl: Boolean = true,
    val audioRoute: AudioRoute = AudioRoute.SPEAKER
)

data class KeyBindingSettings(
    val pttKeyCode: Int = android.view.KeyEvent.KEYCODE_VOLUME_DOWN,
    val isToggleMode: Boolean = false, // false = hold to talk, true = toggle
    val allowVolumeKeyPtt: Boolean = true,
    val headsetHookEnabled: Boolean = true
)

data class DiagnosticLog(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR
}
