package com.example.model

data class RadioChannel(
    val id: String,
    val name: String,
    val frequencyMhz: String,
    val description: String = "",
    val activeUsersCount: Int = 0,
    val currentSpeakerId: String? = null,
    val currentSpeakerName: String? = null,
    val isLocked: Boolean = false
)

data class RadioUser(
    val id: String,
    val callsign: String,
    val username: String,
    val channelId: String,
    val isTransmitting: Boolean = false,
    val isSelf: Boolean = false,
    val pingMs: Long = 0,
    val joinedTimestamp: Long = System.currentTimeMillis()
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class PttState {
    READY,          // Ready to transmit
    PRESSED,        // Button pressed, initializing capture
    TRANSMITTING,   // Audio is actively sending
    RECEIVING,      // Audio is incoming from another user
    BUSY,           // Channel busy (another operator transmitting in half-duplex)
    ERROR           // Hardware, permission, or socket error
}

enum class AudioRoute {
    SPEAKER,
    EARPIECE,
    BLUETOOTH,
    WIRED_HEADSET
}
